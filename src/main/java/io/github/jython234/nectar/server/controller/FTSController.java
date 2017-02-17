package io.github.jython234.nectar.server.controller;

import io.github.jython234.nectar.server.NectarServerApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * Controller to handle FTS methods.
 *
 * @author jython234
 */
@RestController
public class FTSController {

    @RequestMapping(value = NectarServerApplication.ROOT_PATH + "/fts/upload", method = RequestMethod.POST)
    public ResponseEntity upload(@RequestParam(value = "token") String jwtRaw/*, @RequestParam(value = "path") String path
                                , @RequestParam(value = "file") MultipartFile file*/) {

        if(!checkSpace()) {
            return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE).body("FTS directory free space low.");
        }

        System.out.println("Token: " + jwtRaw);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Success.");
    }

    private boolean checkSpace() {
        File ftsDir = new File(NectarServerApplication.getConfiguration().getFtsDirectory());

        long usableSpace = (ftsDir.getFreeSpace() / 1000) / 1000;
        System.out.println(ftsDir.getFreeSpace());
        if(usableSpace <= NectarServerApplication.getConfiguration().getSpaceThreshold()) {
            NectarServerApplication.getLogger().warn("FTS Directory only has " + usableSpace + "MB of free space left!");
            return false;
        }
        return true;
    }
}