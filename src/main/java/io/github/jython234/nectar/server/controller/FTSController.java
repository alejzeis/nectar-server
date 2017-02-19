package io.github.jython234.nectar.server.controller;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import io.github.jython234.nectar.server.NectarServerApplication;
import io.github.jython234.nectar.server.Util;
import io.github.jython234.nectar.server.struct.SessionToken;
import org.apache.commons.io.FileSystemUtils;
import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;

/**
 * Controller to handle FTS methods.
 *
 * @author jython234
 */
@RestController
public class FTSController {

    @RequestMapping(value = NectarServerApplication.ROOT_PATH + "/fts/upload", method = RequestMethod.POST)
    public ResponseEntity upload(@RequestParam(value = "token") String jwtRaw, @RequestParam(value = "path") String path
                                , @RequestParam(value = "name") String name, @RequestParam(value = "public") boolean isPublic
                                , @RequestParam(value = "file") MultipartFile file, HttpServletRequest request) {

        ResponseEntity r = Util.verifyJWT(jwtRaw, request);
        if(r != null)
            return r;

        SessionToken token = SessionToken.fromJSON(Util.getJWTPayload(jwtRaw));

        if(SessionController.getInstance().checkToken(token)) {
            if(!token.isFull()) return ResponseEntity.badRequest().body("Management sessions can't access the FTS.");

            MongoCollection<Document> clients = NectarServerApplication.getDb().getCollection("clients");
            MongoCollection<Document> users = NectarServerApplication.getDb().getCollection("users");
            Document doc = clients.find(Filters.eq("uuid", token.getUuid())).first();

            // Check if the user is logged in ----------------------------------------------------------------------------------------

            if(doc == null)
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to find entry in database for client.");

            String loggedInUser;
            try {
                // getString will throw an exception if the key is not present in the document
                loggedInUser = doc.getString("loggedInUser");
                if (loggedInUser.equals("none")) {
                    // No user is logged in
                    throw new RuntimeException(); // Move to catch block
                }
            } catch(Exception e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Must be logged in to use FTS.");
            }

            // Process Upload ---------------------------------------------------------------------------------------------------------

            if(!checkSpace()) {
                return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE).body("FTS directory free space low.");
            }

            ResponseEntity res;
            if(isPublic) {
                // Need to be admin to upload to public store
                try {
                    ResponseEntity re = AuthController.checkUserAdmin(token, users, doc);
                    // Throws if user is not admin
                    if(re != null)
                        return re;
                } catch(Exception e) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User with admin privilege must be logged in on this client.");
                }

                res = doUpload("publicStore", loggedInUser, name, path, file);
                if(res != null)
                    return res;
            } else {
                res = doUpload("usrStore" + File.separator + loggedInUser, loggedInUser, name, path, file);
                if(res != null)
                    return res;
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token expired/not valid.");
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Success.");
    }

    private ResponseEntity doUpload(String ftsPath, String loggedInUser, String name, String path, MultipartFile file) {
        File uploadPath = new File(NectarServerApplication.getConfiguration().getFtsDirectory() + File.separator + ftsPath);

        if (!uploadPath.exists()) {
            if (!uploadPath.mkdirs()) {
                NectarServerApplication.getLogger().warn("Failed to create directories while processing FTS upload \"" + path + "\""
                        + " from user \"" + loggedInUser + "\""
                );
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("mkdirs() call failed.");
            }
        }

        try {
            FileUtils.copyInputStreamToFile(file.getInputStream(), new File(uploadPath + File.separator + name));
        } catch (IOException e) {
            e.printStackTrace();
            NectarServerApplication.getLogger().error("IOException while processing FTS upload \"" + path + "\""
                    + " from user\"" + loggedInUser + "\""
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("IOException while storing file.");
        }

        return null;
    }

    private boolean checkSpace() {
        File ftsDir = new File(NectarServerApplication.getConfiguration().getFtsDirectory());

        long usableSpace = (ftsDir.getFreeSpace() / 1000) / 1000;
        if(usableSpace <= NectarServerApplication.getConfiguration().getSpaceThreshold()) {
            NectarServerApplication.getLogger().warn("FTS Directory only has " + usableSpace + "MB of free space left!");
            return false;
        }
        return true;
    }
}