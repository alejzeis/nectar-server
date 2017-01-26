import * as busboy from "busboy";
import * as express from "express";
import * as jsonwebtoken from "jsonwebtoken";

import * as fs from "fs";

import * as server from "./server";
import * as util from "./util";

export class FileTransferManager {
    private server: server.Server;
    private rootDir: string;

    constructor(server: server.Server) {
        this.server = server;

        this.checkRootDir();

        this.server.registerGETHandle("fts/download", this.fileDownloadHandler.bind(this));
    }

    private checkRootDir() {
        if(!this.server.config.fts.directory) {
            this.server.logger.error("Invalid Config.");
            this.server.logger.error("Missing config entry: fts.directory");
            process.exit(1);
        }

        this.rootDir = this.server.config.fts.directory;

        // TODO: CHECK ROOT DIR ON WINDOWS

        if(!this.rootDir.startsWith("/")) {
            // Assume relative to config dir path
            this.rootDir = util.getConfigDirLocation(process.env.NECTAR_USE_SYSTEM) + "/" + this.rootDir;
        }

        fs.access(this.rootDir, fs.constants.F_OK | fs.constants.R_OK | fs.constants.W_OK, (err) => {
            if(err) {
                if(!fs.existsSync(this.rootDir)) {
                    fs.mkdir(this.rootDir, (err) => {
                        if(err) {
                            this.server.logger.error("Attempted to create FTS directory: " + this.rootDir +", but failed!");
                            this.server.logger.error("Please check filesystem permissions!");
                            process.exit(1);
                        }
                    });
                }
                this.server.logger.error("Nectar can't see/read/write the FTS directory: " + this.rootDir);
                this.server.logger.error("Please check filesystem permissions!");
                process.exit(1);
            }
        });

        if(!fs.existsSync(this.rootDir + "/public")) {
            fs.mkdir(this.rootDir + "/public", (err) => {
                if(err) {
                    this.server.logger.error("Attempted to create FTS public directory: " + this.rootDir +"/public, but failed!");
                    this.server.logger.error("Please check filesystem permissions!");
                    process.exit(1);
                }
            });
        }
    }

    private fileDownloadHandler(request: express.Request, response: express.Response) {
        if(request.query.token == null || request.query.path == null) {
            response.status(400).send("Missing query items: token, path");
            return;
        }

        jsonwebtoken.verify(request.query.token, this.server.serverPublicKey, (err: any, decoded: any) => {
            if(err) {
                response.status(400).send("Failed to verify token."); // 400: Bad Request
            } else {
                if(!this.server.clients.has(decoded.uuid)) {
                    response.status(403).send("Token Expired!"); // 403: Forbidden
                } else {
                    if(request.query.path.startsWith("/public")) {
                        // Requesting a public file, authentication is not needed
                        var path = this.rootDir + request.query.path;
                        if(fs.existsSync(path)) {
                            response.sendFile(path);
                        } else {
                            response.sendStatus(404);
                        }
                    } else {
                        // Requesting a user file, check authentication and such.
                        // TODO!
                        response.status(501).send("User based FTS and authentication is not implemented.");
                    }
                }
            }
        });
    }
}
