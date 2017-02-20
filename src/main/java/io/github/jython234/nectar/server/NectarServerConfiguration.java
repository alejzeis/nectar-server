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

import lombok.Getter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.ini4j.Ini;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Class which holds the values from the
 * server.ini configuration
 *
 * @author jython234
 */
public class NectarServerConfiguration {
    private static NectarServerConfiguration INSTANCE;

    // Network Section -----------------------------------------
    @Getter private final int bindPort;

    // db Section ----------------------------------------------
    @Getter private final String dbIP;
    @Getter private final int dbPort;
    @Getter private final String dbName;

    // Security Section ----------------------------------------
    @Getter private final String serverPrivateKeyLocation;
    @Getter private final String serverPublicKeyLocation;
    @Getter private final String clientPublicKeyLocation;

    @Getter private ECPrivateKey serverPrivateKey;
    @Getter private ECPublicKey serverPublicKey;

    @Getter private ECPublicKey clientPublicKey;

    // FTS Section ---------------------------------------------
    @Getter private final String ftsDirectory;
    @Getter private final long spaceThreshold;

    NectarServerConfiguration(Ini config) {
        this.bindPort = Integer.parseInt(config.get("network").get("bindPort"));

        this.dbIP = config.get("db").get("ip");
        this.dbPort = Integer.parseInt(config.get("db").get("port"));
        this.dbName = config.get("db").get("name");

        this.serverPrivateKeyLocation = config.get("security").get("serverPrivateKey");
        this.serverPublicKeyLocation = config.get("security").get("serverPublicKey");
        this.clientPublicKeyLocation = config.get("security").get("clientPublicKey");

        String ftsDirectory = config.get("fts").get("directory");

        if(!ftsDirectory.startsWith("/")) { // TODO: Windows Support
            // Check if the FTS directory is relative to the config directory
            this.ftsDirectory = System.getProperty("user.dir") + "/" + ftsDirectory;
        } else this.ftsDirectory = ftsDirectory;

        File ftsDirFile = new File(ftsDirectory);

        if(!ftsDirFile.exists()) {
            if(!ftsDirFile.mkdir()) {
                NectarServerApplication.getLogger().error("Failed to create FTS directory! (mkdir failed)");
                System.exit(1);
            }
        }

        // Create publicStore directory
        if(!new File(this.ftsDirectory + File.separator + "publicStore").mkdir()) {
            NectarServerApplication.getLogger().error("Failed to create FTS publicStore directory! (mkdir failed)");
            System.exit(1);
        }

        // Create usrStore directory
        if(!new File(this.ftsDirectory + File.separator + "usrStore").mkdir()) {
            NectarServerApplication.getLogger().error("Failed to create FTS usrStore directory! (mkdir failed)");
            System.exit(1);
        }

        this.spaceThreshold = Long.parseLong(config.get("fts").get("spaceThreshold"));

        loadKeys();

        INSTANCE = this;
    }

    private void loadKeys() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        this.serverPrivateKey = loadPrivateKey(this.serverPrivateKeyLocation);
        this.serverPublicKey = loadPublicKey(this.serverPublicKeyLocation);
        this.clientPublicKey = loadPublicKey(this.clientPublicKeyLocation);

        NectarServerApplication.getLogger().info("Loaded keys.");
    }

    private ECPrivateKey loadPrivateKey(String location) {
        try {
            PemObject pem = new PemReader(new FileReader(location)).readPemObject();

            return (ECPrivateKey) KeyFactory.getInstance("EC", "BC").generatePrivate(new PKCS8EncodedKeySpec(pem.getContent()));
        } catch (FileNotFoundException e) {
            LoggerFactory.getLogger("Nectar").error("Failed to find Private KEY: " + location);
            System.exit(1);
        } catch (IOException e) {
            LoggerFactory.getLogger("Nectar").error("IOException while loading Private Key!");
            e.printStackTrace();
            System.exit(1);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException e) {
            LoggerFactory.getLogger("Nectar").error("Failed to load private key: " + location);
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    private ECPublicKey loadPublicKey(String location) {
        try {
            PemObject spki = new PemReader(new FileReader(location)).readPemObject();

            return (ECPublicKey) KeyFactory.getInstance("EC", "BC").generatePublic(new X509EncodedKeySpec(spki.getContent()));
        } catch (FileNotFoundException e) {
            LoggerFactory.getLogger("Nectar").error("Failed to find Private KEY: " + location);
            System.exit(1);
        } catch (IOException e) {
            LoggerFactory.getLogger("Nectar").error("IOException while loading Public Key!");
            e.printStackTrace();
            System.exit(1);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    public static NectarServerConfiguration getInstance() {
        return INSTANCE;
    }
}
