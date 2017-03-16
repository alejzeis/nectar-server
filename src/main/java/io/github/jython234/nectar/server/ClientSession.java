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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import io.github.jython234.nectar.server.controller.SessionController;
import io.github.jython234.nectar.server.struct.ClientState;
import io.github.jython234.nectar.server.struct.SessionToken;
import io.github.jython234.nectar.server.struct.operation.ClientOperation;
import io.github.jython234.nectar.server.struct.operation.OperationStatus;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Base64;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Represents a Client Session with
 * a token.
 *
 * @author jython234
 */
public class ClientSession {
    @Getter private SessionToken token;
    @Getter private ClientState state;
    @Getter private long lastPing;

    @Getter @Setter private OperationStatus processingStatus;
    @Getter @Setter private int processingNumber = -1;
    @Getter @Setter private String processingMessage = "IDLE";
    @Getter private Queue<ClientOperation> operationQueue = new ConcurrentLinkedQueue<>();

    @Getter private int updates = -1;
    @Getter private int securityUpdates = -1;

    public ClientSession(SessionToken token) {
        this.token = token;

        this.state = ClientState.UNKNOWN;
        this.processingStatus = OperationStatus.IDLE;

        this.lastPing = System.currentTimeMillis();
    }

    public String constructOperationQueueJWT() {
        JSONObject root = new JSONObject();
        JSONArray array;
        if(this.operationQueue.isEmpty()) {
            array = new JSONArray();
        } else {
            array = this.operationQueue.stream().map(ClientOperation::createJSON).collect(Collectors.toCollection(JSONArray::new));
        }

        root.put("array", array);

        return Jwts.builder()
                .setPayload(root.toJSONString())
                .signWith(SignatureAlgorithm.ES384, NectarServerApplication.getConfiguration().getServerPrivateKey())
                .compact(); // Sign and build the JWT
    }

    public void updateOperationStatus(int operationNumber, OperationStatus opStatus, String message) {
        if(opStatus == OperationStatus.IDLE) {
            this.setProcessingNumber(-1);
            this.setProcessingMessage("IDLE");
        } else {
            this.setProcessingNumber(operationNumber);
            this.setProcessingStatus(opStatus);
            this.setProcessingMessage(message);
        }

        if(opStatus == OperationStatus.IN_PROGRESS) {
            this.getOperationQueue().remove(); // Remove the one from the top of the queue
        }
    }

    public void updateState(ClientState state) {
        NectarServerApplication.getLogger().info("Client " + token.getUuid() + " state updated to: " + state.toString());
        this.state = state;

        MongoCollection<Document> clients = NectarServerApplication.getDb().getCollection("clients");
        clients.updateOne(Filters.eq("uuid", token.getUuid()),
                new Document("$set", new Document("state", state.toInt())));
    }

    public boolean handlePing(String dataRaw) {
        String payload = new String(Base64.getUrlDecoder().decode(dataRaw));

        JSONParser parser = new JSONParser();
        JSONObject obj;
        try {
            obj = (JSONObject) parser.parse(payload);
        } catch (ParseException e) {
            NectarServerApplication.getLogger().warn("Invalid JSON from client ping data from \"" + this.token.getUuid() + "\"");
            return false;
        }

        this.lastPing = System.currentTimeMillis();

        this.updates = ((Long) obj.get("updates")).intValue();
        this.securityUpdates = ((Long) obj.get("securityUpdates")).intValue();

        return true;
    }
}
