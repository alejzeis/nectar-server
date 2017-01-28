package io.github.jython234.nectar.server.controller;

import io.github.jython234.nectar.server.NectarServerApplication;
import io.github.jython234.nectar.server.struct.ServerInformation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Base controller
 */
@RestController
public class RootController {

    @RequestMapping(NectarServerApplication.ROOT_PATH + "/infoRequest")
    public ServerInformation infoRequest() {
        return NectarServerApplication.SERVER_INFORMATION;
    }
}
