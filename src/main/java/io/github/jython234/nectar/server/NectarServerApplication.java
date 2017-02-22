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
package io.github.jython234.nectar.server;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import io.github.jython234.nectar.server.controller.FTSController;
import io.github.jython234.nectar.server.struct.PeerInformation;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Getter;
import org.ini4j.Ini;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Main Application class.
 *
 * @author jython234
 */
@SpringBootApplication
@EnableScheduling
public class NectarServerApplication {
    public static final String SOFTWARE = "Nectar-Server";
    public static final String SOFTWARE_VERSION = "0.1.0-SNAPSHOT";

    public static final int API_VERSION_MAJOR = 2;
    public static final int API_VERSION_MINOR = 3;
    public static final String ROOT_PATH_REAL = "/nectar/api/";
    public static final String ROOT_PATH = ROOT_PATH_REAL + "v/" + API_VERSION_MAJOR + "/" + API_VERSION_MINOR;

    public static final String serverID = UUID.randomUUID().toString();
    public static final PeerInformation SERVER_INFORMATION = generateServerInfo();

    @Getter private static String deploymentHash;

    private static MongoClient mongoClient;
    @Getter private static MongoDatabase db;

    @Getter private static Logger logger;
    @Getter private static String configDir;
    @Getter private static NectarServerConfiguration configuration;

    public static void main(String[] args) {
        logger = LoggerFactory.getLogger("Nectar");

        try {
            System.out.println(Util.getResourceContents("header.txt"));
        } catch(IOException e) {
            // Don't worry about failing to print the header
        }

        logger.info("Starting " + SOFTWARE + " version " + SOFTWARE_VERSION +" implementing API "
                + API_VERSION_MAJOR + "-" + API_VERSION_MINOR);

        logger.info("Server ID is " + serverID);

        try {
            logger.info("Loading configuration...");
            loadConfig();
        } catch (IOException e) {
            System.err.println("Failed to load configuration! IOException");
            e.printStackTrace(System.err);
            System.exit(1);
        }

        try {
            setupDeployment();
        } catch (IOException e) {
            System.err.println("Failed to setup deployment token!");
            e.printStackTrace(System.err);
            System.exit(1);
        }

        logger.info("Connecting to MongoDB database...");

        connectMongo();

        logger.info("Building FTS Checksum index (this could take a while!)...");

        FTSController.buildChecksumIndex();

        logger.info("Done!");

        logger.info("Starting SpringApplication...");

        SpringApplication.run(NectarServerApplication.class, args);
    }

    private static void loadConfig() throws IOException {
        determineConfigDir();

        File configFile = new File(configDir + "/server.ini");
        if(!configFile.exists() || !configFile.isFile()) {
            Util.copyResourceTo("default.ini", configFile);
        }

        Ini conf = new Ini();
        conf.load(configFile);

        configuration = new NectarServerConfiguration(conf);
    }

    private static void determineConfigDir() {
        boolean useSystem = Boolean.parseBoolean(System.getenv("NECTAR_USE_SYSTEM"));
        if(useSystem) {
            configDir = "/etc/nectar-server/"; // TODO: Windows Support
        } else {
            configDir = System.getProperty("user.dir");
        }
    }

    private static void connectMongo() {
        mongoClient = new MongoClient(configuration.getDbIP(), configuration.getDbPort());
        db = mongoClient.getDatabase(configuration.getDbName());

        try {
            mongoClient.getAddress();
        } catch(Exception e) {
            e.printStackTrace();
            logger.error("Failed to connect to MongoDB database!");
            System.exit(1);
        }
    }

    private static void setupDeployment() throws IOException {
        logger.info("Client Deployment System is: " + (configuration.isDeploymentEnabled() ? "ENABLED" : "DISABLED"));
        if(configuration.isDeploymentEnabled()) {
            File tokenFile = new File(configDir + File.separator + "deployToken.txt");
            if(!tokenFile.exists()) {
                generateNewDeploymentToken(tokenFile);
            } else {
                try {
                    String contents = Util.getFileContents(tokenFile).replaceAll("\r", "").replaceAll("\n", "");

                    Jwts.parser().setSigningKey(NectarServerApplication.getConfiguration().getServerPublicKey())
                            .parse(contents); // Verify signature

                    getDeploymentHashFromToken(Util.getJWTPayload(contents));
                } catch(Exception e) {
                    e.printStackTrace();
                    logger.warn("Failed to load deployment token from disk, generating new...");
                    generateNewDeploymentToken(tokenFile);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void generateNewDeploymentToken(File tokenFile) throws IOException {
        deploymentHash = Util.computeSHA256(serverID);

        JSONObject root = new JSONObject();
        root.put("timestamp", System.currentTimeMillis());
        root.put("hash", deploymentHash);

        String jwt = Jwts.builder()
                .setPayload(root.toJSONString())
                .signWith(SignatureAlgorithm.ES384, configuration.getServerPrivateKey())
                .compact(); // Sign and build the JWT

        Util.putFileContents(jwt, tokenFile);

        logger.info("Generated new deployment token.");
    }

    private static void getDeploymentHashFromToken(String payload) throws ParseException {
        JSONParser parser = new JSONParser();

        JSONObject obj = (JSONObject) parser.parse(payload);
        deploymentHash = (String) obj.get("hash");
    }

    private static PeerInformation generateServerInfo() {
        return new PeerInformation(
                SOFTWARE,
                SOFTWARE_VERSION,
                API_VERSION_MAJOR,
                API_VERSION_MINOR,
                serverID,
                new PeerInformation.SystemInfo(
                        System.getProperty("java.version"),
                        System.getenv("os.arch"),
                        System.getenv("os.name"),
                        "unknown", // TODO: Parse /proc/cpuinfo
                        Runtime.getRuntime().availableProcessors()
                )
        );
    }

}
