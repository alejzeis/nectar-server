package io.github.jython234.nectar.server.controller;

import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import io.github.jython234.nectar.server.ClientSession;
import io.github.jython234.nectar.server.NectarServerApplication;
import io.github.jython234.nectar.server.Util;
import io.github.jython234.nectar.server.struct.ClientState;
import io.github.jython234.nectar.server.struct.ManagementSessionToken;
import org.bson.Document;
import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * REST controller which handles queries for various
 * data.
 *
 * @author jython234
 */
@RestController
public class QueryController {

    @RequestMapping(NectarServerApplication.ROOT_PATH + "/query/queryState")
    public ResponseEntity<Integer> queryState(@RequestParam(value = "token") String jwtRaw,
                                              @RequestParam(value = "uuid") String uuid) {

        ManagementSessionToken token = ManagementSessionToken.fromJSON(Util.getJWTPayload(jwtRaw));
        if(token == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(-1);

        if(!SessionController.getInstance().checkManagementToken(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(-1);
        }

        if(SessionController.getInstance().sessions.containsKey(uuid)) {
            return ResponseEntity.ok(SessionController.getInstance().sessions.get(uuid).getState().toInt());
        }

        // The session requested is not connected. Check the database then.
        MongoCollection<Document> clients = NectarServerApplication.getDb().getCollection("clients");
        Document doc = clients.find(Filters.eq("uuid", uuid)).first();
        if(doc == null) {
            // We couldn't find a client with the UUID, that means it's an invalid client UUID
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(-1);
        } else {
            // We found the client
            // Default value is UNKNOWN, in case the state was never set in the database yet.
            return ResponseEntity.ok(doc.getInteger("state", ClientState.UNKNOWN.toInt()));
        }
    }

    public ResponseEntity queryClientUpdateCount(@RequestParam(value = "token") String jwtRaw,
                                                 @RequestParam(value = "uuid") String uuid) {

        ManagementSessionToken token = ManagementSessionToken.fromJSON(Util.getJWTPayload(jwtRaw));
        if(token == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid TOKENTYPE.");

        if(!SessionController.getInstance().checkManagementToken(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token expired/not valid.");
        }

        JSONObject returnJSON = new JSONObject();

        if(SessionController.getInstance().sessions.containsKey(uuid)) {
            returnJSON.put("updates", SessionController.getInstance().sessions.get(uuid).getUpdates());
            returnJSON.put("securityUpdates", SessionController.getInstance().sessions.get(uuid).getSecurityUpdates());

            return ResponseEntity.ok(returnJSON.toJSONString());
        }

        // The session requested is not connected. No data avaliable then.
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Session not found or is not connected.");
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(NectarServerApplication.ROOT_PATH + "/query/queryClients")
    public ResponseEntity queryClients(@RequestParam(value = "token") String jwtRaw, HttpServletRequest request) {
        ManagementSessionToken token = ManagementSessionToken.fromJSON(Util.getJWTPayload(jwtRaw));
        if(token == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid TOKENTYPE.");

        if(!SessionController.getInstance().checkManagementToken(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token expired/not valid.");
        }

        JSONObject returnJSON = new JSONObject();

        MongoCollection<Document> clients = NectarServerApplication.getDb().getCollection("clients");
        clients.find().forEach((Block<Document>) document -> {
            String uuid = document.getString("uuid");
            ClientState state = ClientState.fromInt(document.getInteger("state", ClientState.UNKNOWN.toInt()));

            JSONObject clientJSON = new JSONObject();
            clientJSON.put("state", state.toInt());

            if(SessionController.getInstance().sessions.containsKey(uuid)) {
                // Check if this client is currently online, so we can get update count
                // And operation information
                ClientSession session = SessionController.getInstance().sessions.get(uuid);

                clientJSON.put("updates", session.getUpdates());
                clientJSON.put("securityUpdates", session.getSecurityUpdates());

                clientJSON.put("operationCount", session.getOperationQueue().size());
                clientJSON.put("operationStatus", session.getProcessingStatus().toInt());
            }

            returnJSON.put(uuid, clientJSON);
        });

        return ResponseEntity.ok(returnJSON.toJSONString());
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(NectarServerApplication.ROOT_PATH + "/query/queryUsers")
    public ResponseEntity queryUsers(@RequestParam(value = "token") String jwtRaw, HttpServletRequest request) {
        ManagementSessionToken token = ManagementSessionToken.fromJSON(Util.getJWTPayload(jwtRaw));
        if(token == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid TOKENTYPE.");

        if(!SessionController.getInstance().checkManagementToken(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token expired/not valid.");
        }

        JSONObject returnJSON = new JSONObject();

        MongoCollection<Document> clients = NectarServerApplication.getDb().getCollection("clients");
        MongoCollection<Document> users = NectarServerApplication.getDb().getCollection("users");
        users.find().forEach((Block<Document>) document -> {
            String username = document.getString("username");
            boolean admin = document.getBoolean("admin", false);

            JSONObject userJSON = new JSONObject();
            userJSON.put("admin", admin);

            Document clientDoc = clients.find(Filters.eq("loggedInUser", username)).first();
            if(clientDoc == null) {
                userJSON.put("signedIn", false);
            } else {
                userJSON.put("signedIn", true);
            }

            returnJSON.put(username, userJSON);
        });

        return ResponseEntity.ok(returnJSON.toJSONString());
    }
}
