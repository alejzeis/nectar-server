package io.github.jython234.nectar.server.struct;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;

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

    @SuppressWarnings("unchecked")
    public JSONObject constructJSON() {
        JSONObject root = new JSONObject();
        root.put("uuid", uuid);
        root.put("timestamp", timestamp);
        root.put("expires", expires);

        return root;
    }
}
