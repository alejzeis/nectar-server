import * as express from "express";
import * as jsonwebtoken from "jsonwebtoken";

import * as server from "./server";
import * as auth from "./auth";

export class ClientManager {
    private server: server.Server;

    constructor(server: server.Server) {
        this.server = server;

        this.server.registerGETHandle("client/ping", this.pingRequestHandler.bind(this));
        this.server.registerGETHandle("client/switchState", this.stateRequestHandler.bind(this));
    }

    private pingRequestHandler(request: express.Request, response: express.Response) {
        if(request.query.token == null || request.query.data == null) {
            response.sendStatus(400); // 400: Bad Request
            return;
        }

        jsonwebtoken.verify(request.query.token, this.server.serverPublicKey, (err: any, decoded: any) => {
            if(err) {
                response.status(400).send("Failed to verify token."); // 400: Bad Request
            } else {
                if(!this.server.clients.has(decoded.uuid)) {
                    response.status(403).send("Token Expired!"); // 403: Forbidden
                } else {
                    response.sendStatus(204); // 204: No content
                    this.server.clients.get(decoded.uuid).handlePingData(request.query.data);
                }
            }
        });
    }

    private stateRequestHandler(request: express.Request, response: express.Response) {
        if(request.query.token == null || request.query.state == null) {
            response.sendStatus(400); // 400: Bad Request
            return;
        }

        jsonwebtoken.verify(request.query.token, this.server.serverPublicKey, (err: any, decoded:any) => {
            if(err) {
                response.sendStatus(400).send("Failed to verify token.");
            } else {
                if(!this.server.clients.has(decoded.uuid)) {
                    response.status(403).send("Token Expired!"); // 403: Forbidden
                } else {
                    response.sendStatus(204); // 204: No content
                    this.server.clients.get(decoded.uuid).handleStateChange(request.query.state);
                }
            }
        });
    }
}

export class Client {
    private server: server.Server;

    private _token: any; // This client's token in plain form.
    private _expired: boolean = false;

    private _online: boolean = true;
    private _authenticated: boolean = false; // If the client has authenticated with a user.

    private _securityUpdates: Number;
    private _otherUpdates: Number;

    constructor(server: server.Server, token: any) {
        this.server = server;
        this._token = token;
    }

    public handlePingData(data: any) {
        if(data.securityUpdates) {
            this._securityUpdates = data.securityUpdates;
        }

        if(data.otherUpdates) {
            this._otherUpdates = data.otherUpdates;
        }
    }

    /*
        Possible States:
        0: Normal, client is online
        1: Client is going to sleep. Token will be revoked
        2: Client is shutting down. Token will be revoked
        3: Client is restarting. Token will not be revoked
    */
    public handleStateChange(state: any) {
        var int: Number = parseInt(state);
        if(int !== NaN) {
            switch(int) {
                case 0:
                    this.server.logger.notice("Client " + this.token.uuid + " is now online.");
                    this._online = true;
                    break;
                case 1:
                    this.server.logger.notice("Client " + this.token.uuid + " is going to sleep.");
                    // Revoke token
                    this._online = false;
                    this.expired = true;

                    this.server.clients.delete(this.token.uuid);
                    break;
                case 2:
                    this.server.logger.notice("Client " + this.token.uuid + " is shutting down.");
                    // Revoke token
                    this._online = false;
                    this.expired = true;

                    this.server.clients.delete(this.token.uuid);
                    break;
                case 3:
                    this.server.logger.notice("Client " + this.token.uuid + " is restarting.");
                    this._online = false;
                    break;
                default:
                    this.server.logger.debug("Unknown client state for " + this.token.uuid + ": " + state);
                    break;
            }
        }
    }

    get token(): any {
        return this._token;
    }

    get expired(): boolean {
        return this._expired;
    }

    get online(): boolean {
        return this._online;
    }

    get authenticated(): boolean {
        return this._authenticated;
    }

    set expired(expired: boolean) {
        this._expired = expired;
    }

    get securityUpdates(): Number {
        return this._securityUpdates;
    }

    get otherUpdates(): Number {
        return this._otherUpdates;
    }
}
