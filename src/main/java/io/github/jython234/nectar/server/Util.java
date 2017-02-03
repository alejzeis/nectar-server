package io.github.jython234.nectar.server;

import org.apache.commons.io.FileUtils;

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

    public static String getJWTPayload(String jwtRaw) {
        try {
            return new String(Base64.getDecoder().decode(jwtRaw.split("\\.")[1]));
        } catch(ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("JWT is invalid, missing payload!");
        }
    }
}
