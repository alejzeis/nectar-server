package io.github.jython234.nectar.server.struct;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Represents a Session Token issued
 * by the Server to a client.
 *
 * @author jython234
 */
@RequiredArgsConstructor
public class SessionToken {
    @Getter private final String uuid;
    @Getter private final long timestamp;
    @Getter private final long expires;

    public static SessionToken fromJSON(String json) {
        JSONParser parser = new JSONParser();

        try {
            JSONObject obj = (JSONObject) parser.parse(json);

            if(!obj.containsKey("uuid") || !obj.containsKey("timestamp") || !obj.containsKey("expires"))
                throw new IllegalArgumentException("JSON is invalid: missing keys!");

            return new SessionToken((String) obj.get("uuid"), (long) obj.get("timestamp"), (long) obj.get("expires"));
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public JSONObject constructJSON() {
        JSONObject root = new JSONObject();
        root.put("uuid", uuid);
        root.put("timestamp", timestamp);
        root.put("expires", expires);

        return root;
    }
}
