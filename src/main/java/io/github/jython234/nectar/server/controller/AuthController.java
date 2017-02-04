/*
 * Copyright © 2017, Nectar-Server Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those
 * of the authors and should not be interpreted as representing official policies,
 * either expressed or implied, of the FreeBSD Project.
 */
package io.github.jython234.nectar.server.controller;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import io.github.jython234.nectar.server.NectarServerApplication;
import io.github.jython234.nectar.server.Util;
import io.github.jython234.nectar.server.struct.SessionToken;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.MessageDigest;

/**
 * Controller that handles user
 * authentication, mapped under the
 * "/auth" path.
 *
 * @author jython234
 */
@RestController
public class AuthController {

    @SuppressWarnings("unchecked")
    @RequestMapping(NectarServerApplication.ROOT_PATH + "/auth/login")
    public ResponseEntity<String> login(@RequestParam(value = "token") String jwtRaw, @RequestParam(value = "user") String username,
                                @RequestParam(value = "password") String password, HttpServletRequest request) {

        ResponseEntity r = Util.verifyJWT(jwtRaw, request);
        if(r != null)
            return r;

        SessionToken token = SessionToken.fromJSON(Util.getJWTPayload(jwtRaw));

        if(SessionController.getInstance().checkToken(token)) {
            MongoCollection<Document> clients = NectarServerApplication.getDb().getCollection("clients");
            MongoCollection<Document> users = NectarServerApplication.getDb().getCollection("users");
            Document clientDoc = clients.find(Filters.eq("uuid", token.getUuid())).first();

            if(clientDoc == null) {
                NectarServerApplication.getLogger().warn("Failed to find Client Entry in database for " + token.getUuid());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to find entry in database for client.");
            }

            String loggedInUser;
            try {
                // getString will throw an exception if the key is not present in the document
                loggedInUser = clientDoc.getString("loggedInUser");
                if(loggedInUser.equals("none")) {
                    // No user is logged in
                    throw new RuntimeException(); // Move to catch block
                }

                return ResponseEntity.status(HttpStatus.CONFLICT).body("A User is already logged in under this client!");
            } catch(Exception e) {
                // No user is logged in
                Document userDoc = users.find(Filters.eq("username", username)).first();
                if(userDoc == null) {
                    // The user trying to log in does not exist
                    NectarServerApplication.getLogger().warn("Attempted user login for \"" + username + "\", from "
                            + token.getUuid() + ", user not found in database."
                    );
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found in database!");
                }

                // Check their password
                if(userDoc.getString("password").equals(Util.computeSHA256(password))) {
                    // Password check complete, now update the database with the state
                    clients.updateOne(Filters.eq("uuid", token.getUuid()),
                            new Document("$set", new Document("loggedInUser", username))
                    );
                    NectarServerApplication.getLogger().info("User \"" + username + "\" logged in from " + token.getUuid());
                } else {
                    System.out.println(userDoc.getString("password"));
                    System.out.println(Util.computeSHA256(password));
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Password not correct!");
                }
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token expired/not valid.");
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Success.");
    }

    @RequestMapping(NectarServerApplication.ROOT_PATH + "/auth/logout")
    public ResponseEntity<String> logout(@RequestParam(value = "token") String jwtRaw, HttpServletRequest request) {
        ResponseEntity r = Util.verifyJWT(jwtRaw, request);
        if(r != null)
            return r;

        SessionToken token = SessionToken.fromJSON(Util.getJWTPayload(jwtRaw));

        if(SessionController.getInstance().checkToken(token)) {
            MongoCollection<Document> clients = NectarServerApplication.getDb().getCollection("clients");
            Document clientDoc = clients.find(Filters.eq("uuid", token.getUuid())).first();

            if(clientDoc == null) {
                NectarServerApplication.getLogger().warn("Failed to find Client Entry in database for " + token.getUuid());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to find entry in database for client.");
            }

            String loggedInUser;

            try {
                // getString will throw an exception if the key is not present in the document
                loggedInUser = clientDoc.getString("loggedInUser");
                if(loggedInUser.equals("none")) {
                    // No user is logged in
                    throw new RuntimeException(); // Move to catch block
                }
            } catch(Exception e) {
                return ResponseEntity.badRequest().body("No user is currently logged in!");
            }

            clients.updateOne(Filters.eq("uuid", token.getUuid()),
                    new Document("$set", new Document("loggedInUser", "none"))
            );
            NectarServerApplication.getLogger().info("User \"" + loggedInUser + "\" logged out from " + token.getUuid());
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token expired/not valid.");
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Success.");
    }

    @RequestMapping(value = NectarServerApplication.ROOT_PATH + "/auth/registerClient", method = RequestMethod.POST)
    public ResponseEntity<String> registerClient(@RequestParam(value = "user") String user, @RequestParam(value = "password") String password,
                                                 @RequestParam(value = "clientInfo") String clientInfo, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("Not implemented.");
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = NectarServerApplication.ROOT_PATH + "/auth/registerUser", method = RequestMethod.POST)
    public ResponseEntity<String> registerUser(@RequestParam(value = "token") String jwtRaw, @RequestParam(value = "user") String username,
                                               @RequestParam(value = "password") String password, @RequestParam(value = "admin") boolean admin,
                                               HttpServletRequest request) {
        ResponseEntity r = Util.verifyJWT(jwtRaw, request);
        if(r != null)
            return r;

        SessionToken token = SessionToken.fromJSON(Util.getJWTPayload(jwtRaw));

        if(SessionController.getInstance().checkToken(token)) {
            MongoCollection<Document> clients = NectarServerApplication.getDb().getCollection("clients");
            MongoCollection<Document> users = NectarServerApplication.getDb().getCollection("users");
            Document doc = clients.find(Filters.eq("uuid", token.getUuid())).first();

            if(doc == null)
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to find entry in database for client.");

            try {
                // getString will throw an exception if the key is not present in the document
                String loggedInUser = doc.getString("loggedInUser");
                if(loggedInUser.equals("none")) {
                    // No user is logged in
                    throw new RuntimeException(); // Move to catch block
                }

                Document userDoc = users.find(Filters.eq("username", loggedInUser)).first();

                if(userDoc == null) { // We can't find the logged in user in the users database, strange
                    NectarServerApplication.getLogger().warn("Failed to find logged in user \"" + loggedInUser + "\" for session "
                            + token.getUuid() + " while processing user registration"
                    );
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to find current logged in user in DB.");
                } else { // User found, check admin now
                    if(!userDoc.getBoolean("admin", false)) {
                        NectarServerApplication.getLogger().warn("ATTEMPTED USER REGISTRATION BY NON_ADMIN USER \"" + loggedInUser + "\""
                                + " from session " + token.getUuid()
                        );
                        throw new RuntimeException(); // Move to catch block
                    }
                    // User is confirmed logged in and admin, all checks passed.
                }
            } catch(Exception e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User with admin privlidge must be logged in on this client.");
            }

            if(users.find(Filters.eq("username", username)).first() != null)
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists!");

            clients.insertOne(new Document()
                    .append("username", username)
                    .append("password", Util.computeSHA256(password))
                    .append("admin", admin));

            NectarServerApplication.getLogger().info("Registered new user \"" + username + "\", admin: " + admin + ", from client " + token.getUuid());
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token expired/not valid.");
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Success.");
    }
}
