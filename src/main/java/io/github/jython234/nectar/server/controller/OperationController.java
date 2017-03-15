package io.github.jython234.nectar.server.controller;

import io.github.jython234.nectar.server.ClientSession;
import io.github.jython234.nectar.server.NectarServerApplication;
import io.github.jython234.nectar.server.Util;
import io.github.jython234.nectar.server.struct.SessionToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

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
}
