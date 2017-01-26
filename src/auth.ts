import * as express from "express";
import * as uuid from "uuid";
import * as jsonwebtoken from "jsonwebtoken";

import * as server from "./server";
import * as client from "./client";
import * as util from "./util";

export class AuthManager {
    protected server: server.Server;

    constructor(server: server.Server) {
        this.server = server;

        setInterval(this.checkTokens.bind(this), 1000); // Check for expired tokens every second

        this.server.registerGETHandle("auth/tokenRequest", this.tokenRequestHandler.bind(this));
    }

    private checkTokens() {
        this.server.clients.forEach((value, key, map) => {
            var currentTime = (new Date).getTime();
            if((currentTime - value.token.timestamp) >= value.token.expires) {
                this.server.logger.info("Token for " + key + " has expired.");
                value.expired = true;
                this.server.clients.delete(key);
            }
        });
    }

    private tokenRequestHandler(request: express.Request, response: express.Response) {
        if(request.query.uuid == null || request.query.info == null) {
            response.status(400).send("Missing query items: uuid, info"); // 400: Bad Request
            return;
        }

        if(this.server.clients.has(request.query.uuid)) {
            response.status(400).send("A token has already been issued to this UUID!"); // 400: Bad Request
            return;
        }

        var token = {
            uuid: request.query.uuid,
            timestamp: (new Date).getTime(),
            expires: 1800000 // Token expires in 30 minutes
        }

        var jwt = jsonwebtoken.sign(token, this.server.serverPrivateKey, { algorithm: "ES384" });

        var c: client.Client = new client.Client(this.server, token);
        this.server.clients.set(request.query.uuid, c);

        this.server.logger.info("Issued token for new client with UUID: " + request.query.uuid);

        response.status(200).send(jwt); // 200: OK
    }
}
