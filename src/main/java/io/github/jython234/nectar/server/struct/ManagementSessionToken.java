package io.github.jython234.nectar.server.struct;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Represents a Session Token issued
 * by the Server to a management client.
 * Management sessions are separate from
 * normal sessions, as they can only
 * call management methods.
 *
 * @author jython234
 */
@RequiredArgsConstructor
public class ManagementSessionToken implements Token {
    public static final String TOKEN_TYPE = "MGMT";

    @Getter private final String serverID;
    @Getter private final String clientIP;
    @Getter private final long timestamp;
    @Getter private final long expires;

    public static ManagementSessionToken fromJSON(String json) {
        JSONParser parser = new JSONParser();

        try {
            JSONObject obj = (JSONObject) parser.parse(json);

            if(!obj.containsKey("TOKENTYPE") || !obj.get("TOKENTYPE").equals(TOKEN_TYPE)) {
                return null;
            }

            if(!obj.containsKey("serverID") || !obj.containsKey("clientIP") ||
                    !obj.containsKey("timestamp") || !obj.containsKey("expires"))
                throw new IllegalArgumentException("JSON is invalid: missing keys!");

            return new ManagementSessionToken((String) obj.get("serverID"), (String) obj.get("clientIP"), (long) obj.get("timestamp"), (long) obj.get("expires"));
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public JSONObject constructJSON() {
        JSONObject root = new JSONObject();
        root.put("TOKENTYPE", TOKEN_TYPE);

        root.put("serverID", serverID);
        root.put("clientIP", clientIP);
        root.put("timestamp", timestamp);
        root.put("expires", expires);

        return root;
    }
}
