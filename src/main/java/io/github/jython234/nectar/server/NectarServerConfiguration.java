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
