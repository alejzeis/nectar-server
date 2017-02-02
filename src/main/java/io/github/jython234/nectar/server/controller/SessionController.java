package io.github.jython234.nectar.server.controller;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import io.github.jython234.nectar.server.NectarServerApplication;
import io.github.jython234.nectar.server.struct.SessionToken;
import io.jsonwebtoken.*;
import lombok.AccessLevel;
import lombok.Getter;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller that handles sessions, including
 * token requests and revokes.
 *
 * @author jython234
 */
@RestController
public class SessionController {
    @Getter(AccessLevel.PROTECTED) private static SessionController instance;

    private Map<String, SessionToken> tokens;

    public SessionController() {
        this.tokens = new ConcurrentHashMap<>();

        instance = this;
    }

    @Scheduled(fixedDelay = 500) // Check for tokens every half second
    public void checkTokens() {
        tokens.values().forEach((SessionToken token) -> {
            if((System.currentTimeMillis() - token.getTimestamp()) >= token.getExpires()) { // Check if the token has expired
                // Token has expired, revoke it
                NectarServerApplication.getLogger().info("Token for " + token.getUuid() + " has expired.");
                tokens.remove(token.getUuid());
            }
        });
    }

    /**
     * Checks a SessionToken to see if it is found
     * in the issued tokens map.
     * @param token The token to check.
     * @return If the token has been found and verified issued.
     */
    public boolean checkToken(SessionToken token) {
        for(SessionToken session : tokens.values()) {
            if(session.getUuid().equals(token.getUuid())
                    && session.getTimestamp() == token.getTimestamp()
                    && session.getExpires() == token.getExpires()) {
                return true;
            }
        }

        return false;
    }

    @RequestMapping(NectarServerApplication.ROOT_PATH + "/session/tokenRequest")
    public ResponseEntity<String> tokenRequest(@RequestParam(value="uuid") String uuid, HttpServletRequest request) {
        /*try {
            Jwt jwt = Jwts.parser().setSigningKey(NectarServerApplication.getConfiguration().getClientPublicKey())
                    .parse(info);
        } catch(MalformedJwtException e) {
            NectarServerApplication.getLogger().warn("Malformed JWT from client \"" + uuid
                    + "\" while processing token request."
            );
            return ResponseEntity.badRequest().body("JWT \"clientInfo\" is malformed!");
        } catch(SignatureException e) {
            NectarServerApplication.getLogger().warn("Invalid JWT signature from client \"" + uuid
                    + "\" while processing token request."
            );
            return ResponseEntity.badRequest().body("JWT \"clientInfo\" signature is invalid!");
        } catch(Exception e) {
            NectarServerApplication.getLogger().error(" Failed to verify JWT from client \"" + uuid
                    + "\" while processing token request."
            );
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to verify JWT.");
        }*/

        // Verify "uuid"
        // TODO: Check MongoDB database for UUID

        MongoCollection<Document> clients = NectarServerApplication.getDb().getCollection("clients");
        Document doc = clients.find(Filters.eq("uuid", uuid)).first();
        if(doc == null) {
            // We can't find this client in the database
            // This means that the client is unregistered, so we drop the request
            NectarServerApplication.getLogger().warn("Received token request from unregistered client "
                    + request.getRemoteAddr() + " with UUID: " + uuid
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("UUID not found in database.");
        }

        try {
            UUID.fromString(uuid);
        } catch(IllegalArgumentException e) {
            // UUID is invalid
            return ResponseEntity.badRequest().body("Invalid UUID!");
        }

        // Check if we have issued a token already for this UUID
        if(this.tokens.containsKey(uuid)) {
            // Token has been issued
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token already issued for this UUID!");
        }

        SessionToken token = new SessionToken(uuid, System.currentTimeMillis(), 500);
        this.tokens.put(uuid, token); // Insert the token into the Map

        String jwt = Jwts.builder()
                .setPayload(token.constructJSON().toJSONString())
                .signWith(SignatureAlgorithm.ES384, NectarServerApplication.getConfiguration().getServerPrivateKey())
                .compact(); // Sign and build the JWT

        NectarServerApplication.getLogger().info("Issued token for new client " + request.getRemoteAddr() + " with UUID: " + uuid);

        return ResponseEntity.ok(jwt); // Return the token
    }
}
