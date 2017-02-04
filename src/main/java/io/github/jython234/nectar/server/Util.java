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

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import org.apache.catalina.realm.MessageDigestCredentialHandler;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Misc. Utility methods class
 *
 * @author jython234
 */
public class Util {

    /**
     * Copies a "resource" file from the JAR or resource folder to
     * the filesystem.
     * @param resource The resource file name.
     * @param copyLocation The location to copy the resource to on the
     *                     filesystem.
     * @throws IOException If there is an exception while attempting to
     *                     copy the file.
     */
    public static void copyResourceTo(String resource, File copyLocation) throws IOException {
        InputStream in = ClassLoader.getSystemResourceAsStream("default.ini");
        FileUtils.copyInputStreamToFile(in, copyLocation);
    }

    /**
     * Read the full contents of a "resource" file as
     * a String.
     * @param resource The name of the resource file.
     * @return The full contents of the resource file as a String.
     */
    public static String getResourceContents(String resource) throws IOException {
        InputStream in = ClassLoader.getSystemResourceAsStream(resource);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        String line;
        StringBuilder sb = new StringBuilder();
        while((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }

        return sb.toString();
    }

    /**
     * Get the payload section of a JWT (JSON
     * web token). A JWT has a header, payload,
     * and signature, all base64 encoded.
     * @param jwtRaw The raw JWT string.
     * @return The payload of the JWT
     * @throws IllegalArgumentException if the JWT is invalid and
     *                                  doesn't contain a payload.
     */
    public static String getJWTPayload(String jwtRaw) {
        try {
            return new String(Base64.getDecoder().decode(jwtRaw.split("\\.")[1]));
        } catch(ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("JWT is invalid, missing payload!");
        }
    }

    /**
     * A Utility Method used by the SpringBoot
     * controllers to verify JWTs.
     * @param jwtRaw The raw JWT string.
     * @param request The HTTP request currently being processed.
     * @return A ResponseEntity if the verification failed, or null if succeeded.
     */
    public static ResponseEntity verifyJWT(String jwtRaw, HttpServletRequest request) {
        try {
            Jwts.parser().setSigningKey(NectarServerApplication.getConfiguration().getServerPublicKey())
                    .parse(jwtRaw); // Verify signature
        } catch(MalformedJwtException e) {
            NectarServerApplication.getLogger().warn("Malformed JWT from client \"" + request.getRemoteAddr());
            return ResponseEntity.badRequest().body("JWT is malformed!");
        } catch(SignatureException e) {
            NectarServerApplication.getLogger().warn("Invalid JWT signature from client \"" + request.getRemoteAddr());
            return ResponseEntity.badRequest().body("JWT signature is invalid!");
        } catch(Exception e) {
            NectarServerApplication.getLogger().error("Failed to verify JWT from client \"" + request.getRemoteAddr());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to verify JWT.");
        }

        return null;
    }

    /**
     * Compute the SHA-256 Hash of a String.
     * @param plaintext The raw text to be hashed.
     * @return The hash of the provided text.
     */
    public static String computeSHA256(String plaintext) {
        return DigestUtils.sha256Hex(plaintext);
    }
}
