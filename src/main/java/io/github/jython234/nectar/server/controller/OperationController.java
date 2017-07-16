package io.github.jython234.nectar.server.controller;

import io.github.jython234.nectar.server.ClientSession;
import io.github.jython234.nectar.server.EventLog;
import io.github.jython234.nectar.server.NectarServerApplication;
import io.github.jython234.nectar.server.Util;
import io.github.jython234.nectar.server.struct.ManagementSessionToken;
import io.github.jython234.nectar.server.struct.SessionToken;
import io.github.jython234.nectar.server.struct.operation.ClientOperation;
import io.github.jython234.nectar.server.struct.operation.OperationID;
import io.github.jython234.nectar.server.struct.operation.OperationStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Base64;

/**
 * The REST Controller which handles the Operation
 * subsystem of the Nectar API.
 *
 * @author jython234
 */
@RestController
public class OperationController {

    @SuppressWarnings("unchecked")
    @RequestMapping(NectarServerApplication.ROOT_PATH + "/operation/getQueue")
    public ResponseEntity<String> getQueue(@RequestParam(value = "token") String jwtRaw, HttpServletRequest request) {
        ResponseEntity r = Util.verifyJWT(jwtRaw, request);
        if(r != null)
            return r;

        SessionToken token = SessionToken.fromJSON(Util.getJWTPayload(jwtRaw));
        if(token == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid TOKENTYPE.");

        if(SessionController.getInstance().checkToken(token)) { // Check if the token has expired
            ClientSession session = SessionController.getInstance().sessions.get(token.getUuid());
            return ResponseEntity.ok(session.constructOperationQueueJWT());
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token expired/not valid.");
        }
    }

    @RequestMapping(NectarServerApplication.ROOT_PATH + "/operation/updateStatus")
    public ResponseEntity updateStatus(@RequestParam(value = "token") String jwtRaw, @RequestParam(value = "status") String status,
                                       HttpServletRequest request) {

        ResponseEntity r = Util.verifyJWT(jwtRaw, request);
        if(r != null)
            return r;

        SessionToken token = SessionToken.fromJSON(Util.getJWTPayload(jwtRaw));
        if(token == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid TOKENTYPE.");

        if(SessionController.getInstance().checkToken(token)) { // Check if the token has expired
            ClientSession session = SessionController.getInstance().sessions.get(token.getUuid());

            String decoded = new String(Base64.getUrlDecoder().decode(status));
            JSONParser parser = new JSONParser();

            JSONObject obj;
            try {
                obj = (JSONObject) parser.parse(decoded);
            } catch (ParseException e) {
                NectarServerApplication.getLogger().warn("Failed to parse status JSON from " + session.getToken().getUuid());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to parse status JSON.");
            }

            int number = ((Long) obj.get("operationNumber")).intValue();
            OperationStatus opStatus = OperationStatus.fromInt(((Long) obj.get("state")).intValue());
            String message = (String) obj.get("message");

            session.updateOperationStatus(number, opStatus, message);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token expired/not valid.");
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Success.");
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = NectarServerApplication.ROOT_PATH + "/operation/addToQueue", method = RequestMethod.POST)
    public ResponseEntity addToQueue(@RequestParam(value = "token") String jwtRaw, @RequestParam(value = "opData") String operationDataRaw, HttpServletRequest request) {
        ResponseEntity r = Util.verifyJWT(jwtRaw, request);
        if(r != null)
            return r;

        ManagementSessionToken token = ManagementSessionToken.fromJSON(Util.getJWTPayload(jwtRaw));
        if(token == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid TOKENTYPE.");

        if(SessionController.getInstance().checkManagementToken(token)) { // Check if the token has expired
            String decoded = new String(Base64.getUrlDecoder().decode(operationDataRaw));
            JSONParser parser = new JSONParser();

            JSONObject obj;
            try {
                obj = (JSONObject) parser.parse(decoded);
            } catch (ParseException e) {
                NectarServerApplication.getLogger().warn("Failed to parse opData JSON from " + request.getRemoteAddr());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to parse status JSON.");
            }

            int id = ((Long) obj.get("id")).intValue();

            OperationID opId;
            try {
                opId = OperationID.fromInt(id);
            } catch(IllegalArgumentException e) {
                NectarServerApplication.getLogger().warn("Invalid operation ID in addToQueue from " + request.getRemoteAddr());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid operation ID.");
            }

            if(opId == OperationID.OPERATION_UPDATE_CLIENT_EXECUTABLE && !NectarServerApplication.getConfiguration().isClientExecutableUpdatingEnabled()) {
                NectarServerApplication.getEventLog().logEntry(EventLog.EntryLevel.NOTICE, "Client Executable Update operation attempt from "
                        + request.getRemoteAddr() + ", but client executable updating is disabled on this server.");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Client Executable Updating is not enabled on this server.");
            }

            JSONArray targetsArray = (JSONArray) obj.get("targets");
            JSONObject additionalData = (JSONObject) obj.getOrDefault("additionalData", new JSONObject());

            if(targetsArray.isEmpty())
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No targets were provided (perhaps they are all offline?)");

            for(Object target : targetsArray) {
                String uuid = (String) target;
                if(!SessionController.getInstance().sessions.containsKey(uuid))
                    continue;

                ClientSession session = SessionController.getInstance().sessions.get(uuid);

                session.getOperationQueue().add(
                        new ClientOperation(session.getNextOperationId(), opId, additionalData)
                );
                session.setNextOperationId(session.getNextOperationId() + 1);
            }

            NectarServerApplication.getEventLog().logEntry(EventLog.EntryLevel.INFO, "Added operation " + opId.name() + " to queue for " + targetsArray.size() + " client(s), traced from " + request.getRemoteAddr());
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token expired/not valid.");
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Success.");
    }
}
