package io.github.jython234.nectar.server.controller;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import io.github.jython234.nectar.server.NectarServerApplication;
import io.github.jython234.nectar.server.Util;
import io.github.jython234.nectar.server.struct.IndexJSON;
import io.github.jython234.nectar.server.struct.SessionToken;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.print.Doc;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller to handle FTS methods.
 *
 * @author jython234
 */
@RestController
public class FTSController {

    public static void buildChecksumIndex() {
        File publicDir = new File(NectarServerApplication.getConfiguration().getFtsDirectory() + File.separator + "publicStore");
        File usrDir = new File(NectarServerApplication.getConfiguration().getFtsDirectory() + File.separator + "usrStore");

        MongoCollection<Document> index = NectarServerApplication.getDb().getCollection("ftsIndex");

        try {
            buildChecksumDir(publicDir, true, index);
            buildChecksumDir(usrDir, false, index);
        } catch (IOException e) {
            e.printStackTrace();
            NectarServerApplication.getLogger().error("FAILED TO COMPUTE FTS CHECKSUMS!");
            System.exit(1);
        }
    }

    // TODO: Clean database of entries of deleted files (only because they could be deleted while the server is offline)
    private static void buildChecksumDir(File dir, boolean isPublic, MongoCollection<Document> index) throws IOException {
        File[] contents = dir.listFiles();
        if(contents == null) return;

        List<Document> toInsert = new ArrayList<>();

        for(File file : contents) {
            if(file.isDirectory()) {
                // Recursion: build for all in that directory
                buildChecksumDir(file, isPublic, index);
            } else {
                // Is a file, build the checksum then
                String checksum = Util.computeFileSHA256Checksum(file);
                Document fileDoc = index.find(Filters.eq("path", file.getAbsolutePath())).first();
                if(fileDoc == null) {
                    toInsert.add(new Document()
                            .append("path", file.getAbsolutePath())
                            .append("storePath", Util.absoluteFTSToRelativeStore(file.getAbsolutePath()))
                            .append("isPublic", isPublic)
                            .append("checksum", checksum)
                            .append("lastUpdatedBy", "server"));
                } else {
                    String dbChecksum = fileDoc.getString("checksum");
                    if(!checksum.equals(dbChecksum)) {
                        // Checksum has changed, we assume the file has been changed by the server
                        // This is because if a client changes it, the database will be updated
                        index.updateOne(Filters.eq("path", file.getAbsolutePath()),
                                new Document("$set", new Document("checksum", checksum))
                                ); // Update the checksum into the database

                        index.updateOne(Filters.eq("path", file.getAbsolutePath()),
                                new Document("$set", new Document("lastUpdatedBy", "server"))
                                ); // Change lastUpdatedBy to "server"
                    }
                    // else: Checksum has not changed, all is well
                }
            }
        }

        if(!toInsert.isEmpty()) {
            index.insertMany(toInsert);
        }
    }

    @RequestMapping(value = NectarServerApplication.ROOT_PATH + "/fts/upload", method = RequestMethod.POST)
    public ResponseEntity upload(@RequestParam(value = "token") String jwtRaw, @RequestParam(value = "path") String path
                                , @RequestParam(value = "name") String name, @RequestParam(value = "public") boolean isPublic
                                , @RequestParam(value = "file") MultipartFile file, HttpServletRequest request) {

        ResponseEntity r = Util.verifyJWT(jwtRaw, request);
        if(r != null)
            return r;

        SessionToken token = SessionToken.fromJSON(Util.getJWTPayload(jwtRaw));

        if(SessionController.getInstance().checkToken(token)) {
            if(!token.isFull()) return ResponseEntity.badRequest().body("Management sessions can't upload to the FTS.");

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

            if(!checkSpace(file.getSize())) {
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

                res = doUpload("publicStore", loggedInUser, name, path, true, file);
                if(res != null)
                    return res;
            } else {
                res = doUpload("usrStore" + File.separator + loggedInUser, loggedInUser, name, path, false, file);
                if(res != null)
                    return res;
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token expired/not valid.");
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Success.");
    }

    @RequestMapping(NectarServerApplication.ROOT_PATH + "/fts/download")
    public void download(@RequestParam(value = "token") String jwtRaw, @RequestParam(value = "public") boolean isPublic
                                    , @RequestParam(value = "path") String path, HttpServletRequest request, HttpServletResponse response) {
        ResponseEntity r = Util.verifyJWT(jwtRaw, request);
        if (r != null) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return;
        }

        SessionToken token = SessionToken.fromJSON(Util.getJWTPayload(jwtRaw));

        if(SessionController.getInstance().checkToken(token)) {
            if(!token.isFull()) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                // Management sessions can't download from the FTS.
                return;
            }

            MongoCollection<Document> clients = NectarServerApplication.getDb().getCollection("clients");
            Document doc = clients.find(Filters.eq("uuid", token.getUuid())).first();

            // Check if the user is logged in ----------------------------------------------------------------------------------------

            if(doc == null)
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

            if(isPublic) {
                // You don't need to be logged in to access the public store
                File ftsPath = new File(NectarServerApplication.getConfiguration().getFtsDirectory() + File.separator + "publicStore"
                        + File.separator + path);

                if(!ftsPath.exists()) {
                    response.setStatus(HttpStatus.NOT_FOUND.value());
                    return;
                } else if(ftsPath.isDirectory()) {
                    response.setStatus(HttpStatus.BAD_REQUEST.value());
                    return;
                } else {
                    doDownload(ftsPath, response);
                    return;
                }
            }

            // Client is accessing user store, check for logged in then.

            String loggedInUser;
            try {
                // getString will throw an exception if the key is not present in the document
                loggedInUser = doc.getString("loggedInUser");
                if (loggedInUser.equals("none")) {
                    // No user is logged in
                    throw new RuntimeException(); // Move to catch block
                }
            } catch(Exception e) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                return;
            }

            // User is logged in, now process the download.
            // A user can't access another's data store because the path is specifically tied to the logged in name

            File ftsPath = new File(NectarServerApplication.getConfiguration().getFtsDirectory() + File.separator + "usrStore"
                    + File.separator + loggedInUser + File.separator + path);

            if(!ftsPath.exists()) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
            } else if(ftsPath.isDirectory()) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
            } else {
                doDownload(ftsPath, response);
            }
        } else {
            response.setStatus(HttpStatus.FORBIDDEN.value());
        }
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(NectarServerApplication.ROOT_PATH + "/fts/checksumIndex")
    public ResponseEntity checksumIndex(@RequestParam(value = "token") String jwtRaw, @RequestParam(value = "public") boolean isPublic,
                                                HttpServletRequest request) {

        ResponseEntity r = Util.verifyJWT(jwtRaw, request);
        if(r != null)
            return r;

        SessionToken token = SessionToken.fromJSON(Util.getJWTPayload(jwtRaw));

        if(SessionController.getInstance().checkToken(token)) {
            MongoCollection<Document> index = NectarServerApplication.getDb().getCollection("ftsIndex");
            MongoCollection<Document> clients = NectarServerApplication.getDb().getCollection("clients");
            Document doc = clients.find(Filters.eq("uuid", token.getUuid())).first();

            if(isPublic) {
                // Public store, no user needs to be logged in
                return ResponseEntity.status(HttpStatus.OK).body(constructIndexJSON(index, true, null));
            } else {
                // User's store, we need to check if they are logged in.
                String loggedInUser;
                try {
                    // getString will throw an exception if the key is not present in the document
                    loggedInUser = doc.getString("loggedInUser");
                    if (loggedInUser.equals("none")) {
                        // No user is logged in
                        throw new RuntimeException(); // Move to catch block
                    }
                } catch(Exception e) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("A user needs to be logged in!");
                }

                return ResponseEntity.status(HttpStatus.OK).body(constructIndexJSON(index, false, loggedInUser));
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token expired/not valid.");
        }
    }

    private void doDownload(File ftsPath, HttpServletResponse response) {
        try {
            response.setStatus(HttpStatus.OK.value());
            response.setContentType("application/octet-stream");

            IOUtils.copy(new FileInputStream(ftsPath), response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
            NectarServerApplication.getLogger().warn("IOException while processing FTS download \"" + ftsPath + "\"");
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private ResponseEntity doUpload(String ftsPath, String loggedInUser, String name, String path, boolean isPublic, MultipartFile file) {
        File uploadPath = new File(NectarServerApplication.getConfiguration().getFtsDirectory() + File.separator + ftsPath);
        MongoCollection<Document> index = NectarServerApplication.getDb().getCollection("ftsIndex");

        if (!uploadPath.exists()) {
            if (!uploadPath.mkdirs()) {
                NectarServerApplication.getLogger().warn("Failed to create directories while processing FTS upload \"" + path + "\""
                        + " from user \"" + loggedInUser + "\""
                );
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("mkdirs() call failed.");
            }
        }

        File physicalFile = new File(uploadPath + File.separator + name);
        try {
            FileUtils.copyInputStreamToFile(file.getInputStream(), physicalFile);
        } catch (IOException e) {
            e.printStackTrace();
            NectarServerApplication.getLogger().error("IOException while processing FTS upload \"" + path + "\""
                    + " from user \"" + loggedInUser + "\""
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("IOException while storing file.");
        }

        // Update index with new checksum -----------------------------------------------------------------------------------------------
        String checksum;
        try {
            checksum = Util.computeFileSHA256Checksum(physicalFile);
        } catch (IOException e) {
            e.printStackTrace();
            NectarServerApplication.getLogger().error("IOException while calculating FTS checksum! Upload \"" + path + "\""
                    + " from user \"" + loggedInUser + "\""
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("IOException while calculating checksum.");
        }

        Document doc = index.find(Filters.eq("path", physicalFile.getAbsolutePath())).first();
        if(doc == null) {
            // This is a new upload, create a new document in the index
            index.insertOne(new Document()
                    .append("path", physicalFile.getAbsolutePath())
                    .append("storePath", Util.absoluteFTSToRelativeStore(physicalFile.getAbsolutePath()))
                    .append("isPublic", isPublic)
                    .append("checksum", checksum)
                    .append("lastUpdatedBy", "client")
            );
        } else {
            // Document already exists, time to update the checksum and lastUpdatedBy
            index.updateOne(Filters.eq("path", physicalFile.getAbsolutePath()),
                    new Document("$set", new Document("checksum", checksum))
            );

            index.updateOne(Filters.eq("path", physicalFile.getAbsolutePath()),
                    new Document("$set", new Document("lastUpdatedBy", "client"))
            );
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private IndexJSON[] constructIndexJSON(MongoCollection<Document> index, boolean isPublic, String loggedInUser) {
        List<IndexJSON> list = new ArrayList();

        index.find(Filters.eq("isPublic", isPublic)).forEach((Consumer<? super Document>) (Document doc) -> {
            if(doc.getString("storePath").startsWith(loggedInUser) && !isPublic) {
                // It's the user store
                list.add(
                        new IndexJSON(doc.getString("storePath"), doc.getString("checksum"), doc.getString("lastUpdatedBy")));
            } else if(isPublic)
                list.add(
                    new IndexJSON(doc.getString("storePath"), doc.getString("checksum"), doc.getString("lastUpdatedBy")));
        });

        return list.toArray(new IndexJSON[list.size()]);
    }

    private boolean checkSpace(long size) {
        File ftsDir = new File(NectarServerApplication.getConfiguration().getFtsDirectory());

        long usableSpace = (ftsDir.getFreeSpace() / 1000) / 1000;
        if(usableSpace <= NectarServerApplication.getConfiguration().getSpaceThreshold()) {
            NectarServerApplication.getLogger().warn("FTS Directory only has " + usableSpace + "MB of free space left!");
            return false;
        }

        long sizeMb = size / 1000 / 1000;
        if(usableSpace <= sizeMb) {
            NectarServerApplication.getLogger().warn("Rejected file upload of size " + sizeMb +": not enough space!");
            return false;
        }

        return true;
    }
}