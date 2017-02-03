package io.github.jython234.nectar.server;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import io.github.jython234.nectar.server.controller.SessionController;
import io.github.jython234.nectar.server.struct.ClientState;
import io.github.jython234.nectar.server.struct.SessionToken;
import lombok.Getter;
import org.bson.Document;

/**
 * Represents a Client Session with
 * a token.
 *
 * @author jython234
 */
public class ClientSession {
    @Getter private SessionToken token;
    @Getter private ClientState state;

    public ClientSession(SessionToken token) {
        this.token = token;
        this.state = ClientState.UNKNOWN;
    }

    public void updateState(ClientState state) {
        NectarServerApplication.getLogger().info("Client " + token.getUuid() + " state updated to: " + state.toString());
        this.state = state;

        MongoCollection<Document> clients = NectarServerApplication.getDb().getCollection("clients");
        Document doc = clients.find(Filters.eq("uuid", token.getUuid())).first();
        doc.put("state", state.toInt());
    }
}
