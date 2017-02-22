package io.github.jython234.nectar.server.controller;

import io.github.jython234.nectar.server.NectarServerApplication;
import io.github.jython234.nectar.server.Util;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller that handles deployment operations.
 * This includes registering new clients based
 * on the server's deployment token. By default
 * the Deployment system is DISABLED. It can
 * be enabled in server.ini
 *
 * @author jython234
 */
@RestController
public class DeployController {

    @RequestMapping(NectarServerApplication.ROOT_PATH + "/deploy/deployJoin")
    public ResponseEntity deployJoin(@RequestParam("token") String jwtRaw, HttpServletRequest request) {
        if(!NectarServerApplication.getConfiguration().isDeploymentEnabled())
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Deployment is not enabled on this server.");

        ResponseEntity r = Util.verifyJWT(jwtRaw, request);
        if(r != null)
            return r;

        NectarServerApplication.getLogger().info("Processing deployment join from " + request.getRemoteAddr() + " ...");

        // Extract deploymentHash from token
        JSONParser parser = new JSONParser();
        JSONObject obj;
        try {
            obj = (JSONObject) parser.parse(Util.getJWTPayload(jwtRaw));
        } catch (ParseException e) {
            NectarServerApplication.getLogger().warn("Deployment join failed from " + request.getRemoteAddr() + " (parse failed).");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to get payload from JWT.");
        }
        String deploymentHash = (String) obj.get("hash");

        if(!deploymentHash.equals(NectarServerApplication.getDeploymentHash())) {
            NectarServerApplication.getLogger().warn("Deployment hash mismatch from " + request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Deployment Hash mismatch!");
        }

        // Deployment hashes match, token is valid
        // Generate a new client

        return ResponseEntity.ok(AuthController.registerClientToDatabase(request.getRemoteAddr()).toJSONString());
    }
}
