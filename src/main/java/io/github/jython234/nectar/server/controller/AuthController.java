package io.github.jython234.nectar.server.controller;

import io.github.jython234.nectar.server.NectarServerApplication;
import io.github.jython234.nectar.server.struct.SessionToken;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller that handles user
 * authentication, mapped under the
 * "/auth" path.
 *
 * @author jython234
 */
@RestController
public class AuthController {

    @RequestMapping(NectarServerApplication.ROOT_PATH + "/auth/login")
    public ResponseEntity login(@RequestParam(value = "token") String jwtRaw, @RequestParam(value = "user") String user,
                                @RequestParam(value = "password") String password, HttpServletRequest request) {
        Jwt jwt;
        try {
            jwt = Jwts.parser().setSigningKey(NectarServerApplication.getConfiguration().getClientPublicKey())
                    .parse(jwtRaw);
        } catch(MalformedJwtException e) {
            NectarServerApplication.getLogger().warn("Malformed JWT from client \"" + request.getRemoteAddr()
                    + "\" while processing token request."
            );
            return ResponseEntity.badRequest().body("JWT \"clientInfo\" is malformed!");
        } catch(SignatureException e) {
            NectarServerApplication.getLogger().warn("Invalid JWT signature from client \"" + request.getRemoteAddr()
                    + "\" while processing token request."
            );
            return ResponseEntity.badRequest().body("JWT \"clientInfo\" signature is invalid!");
        } catch(Exception e) {
            NectarServerApplication.getLogger().error(" Failed to verify JWT from client \"" + request.getRemoteAddr()
                    + "\" while processing token request."
            );
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to verify JWT.");
        }

        SessionToken token = SessionToken.fromJSON((String) jwt.getBody());

        if(SessionController.getInstance().checkToken(token)) {
            // TODO: process auth
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token expired/not valid.");
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Success.");
    }
}
