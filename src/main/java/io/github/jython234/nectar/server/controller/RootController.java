package io.github.jython234.nectar.server.controller;

import io.github.jython234.nectar.server.NectarServerApplication;
import io.github.jython234.nectar.server.struct.PeerInformation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Root Controller for the path /nectar/api/[major]/[minor]/
 *
 * @author jython234
 */
@RestController
public class RootController {

    @RequestMapping(NectarServerApplication.ROOT_PATH + "/infoRequest")
    public PeerInformation infoRequest() {
        return NectarServerApplication.SERVER_INFORMATION;
    }
}
