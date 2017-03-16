package io.github.jython234.nectar.server.struct.operation;

import io.github.jython234.nectar.server.NectarServerApplication;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;

/**
 * Represents an Operation for a client to complete.
 *
 * @author jython234
 */
@RequiredArgsConstructor
public class ClientOperation {
    @Getter private final int operationNumber;
    @Getter private final OperationID id;
    @Getter private final JSONObject payload;

    @SuppressWarnings("unchecked")
    public JSONObject createJSON() {
        JSONObject root = new JSONObject();
        root.put("operationNumber", this.operationNumber);
        root.put("id", this.id.toInt());
        root.put("payload", this.payload);
        return root;
    }
}
