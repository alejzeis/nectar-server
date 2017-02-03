package io.github.jython234.nectar.server;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
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
}
