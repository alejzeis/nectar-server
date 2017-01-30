package io.github.jython234.nectar.server.controller;

import io.github.jython234.nectar.server.NectarServerApplication;
import io.github.jython234.nectar.server.struct.PeerInformation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Controller that handles sessions, including
 * token requests and revokes.
 *
 * @author jython234
 */
@RestController
public class SessionController {

    @RequestMapping(NectarServerApplication.ROOT_PATH + "/session/tokenRequest")
    public String tokenRequest(@RequestParam(value="uuid") String uuid, @RequestParam(value="clientInfo") PeerInformation info) {
        return "hello";
    }
}
