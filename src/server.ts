import * as express from "express";
import * as helmet from "helmet";
import * as mongo from "mongodb";
import * as winston from "winston";

import * as fs from "fs";
import * as os from "os";
import * as readline from "readline";

import * as auth from "./auth";
import * as client from "./client";
import * as fts from "./fts";
import * as util from "./util";

export const SOFTWARE = "Nectar-Server"
export const SOFTWARE_VERSION = "0.1.2-alpha1";
export const API_VERSION_MAJOR = "1";
export const API_VERSION_MINOR = "2";

function setupConsole(server: Server) {
    const rl = readline.createInterface({
        input: process.stdin,
        output: process.stdout
    });

    rl.on("SIGINT", () => {
        server.shutdown();
    });
}

export class Server {
    public logger: winston.LoggerInstance;
    public config: any;
    protected info: any;

    protected app: express.Application;
    protected appServer: any;
    protected db: mongo.Db;

    private _serverPrivateKey: any;
    private _serverPublicKey: any;

    private _clientPrivateKey: any;
    private _clientPublicKey: any;

    private _clients: Map<string, client.Client>;

    private _authManager: auth.AuthManager;
    private _clientManager: client.ClientManager;
    private _fileTransferManager: fts.FileTransferManager;

    // SETUP Methods ------------------------------------------------------------------------------------------------------

    private loadConfig() {
        var configLocation: string;

        if(process.env.NECTAR_USE_SYSTEM) { // Check if the system enviorment variable is set.
            configLocation = util.getConfigFileLocation(true);
        } else {
            configLocation = util.getConfigFileLocation(false);
        }

        this.config = util.loadConfig(configLocation);

        this.logger.notice("Loaded configuration from " + configLocation);
    }

    private loadKeys() {
        var privateLocation: string = this.config.security.serverPrivateKey;
        var publicLocation: string = this.config.security.serverPublicKey;

        var privateLocation2: string = this.config.security.clientPrivateKey;
        var publicLocation2: string = this.config.security.clientPublicKey;

        if(privateLocation == null || publicLocation == null) {
            this.logger.error("Invalid config.");
            this.logger.error("Missing required fields security.serverPublicKey OR security.serverPrivateKey.");
            process.exit(1);
        }

        if(privateLocation2 == null || publicLocation2 == null) {
            this.logger.error("Invalid config.");
            this.logger.error("Missing required fields security.clientPublicKey OR security.clientPrivateKey.");
            process.exit(1);
        }

        if(!fs.existsSync(privateLocation) || !fs.existsSync(publicLocation) || !fs.existsSync(privateLocation2) || !fs.existsSync(publicLocation2)) {
            this.logger.error("FAILED TO FIND ES384 KEYS!");
            this.logger.error("Could not find key files as specified in config.");
            this.logger.error("Please generate the keys first using the provided script.");
            process.exit(1);
        }

        this._serverPrivateKey = fs.readFileSync(privateLocation);
        this._serverPublicKey = fs.readFileSync(publicLocation);

        this._clientPrivateKey = fs.readFileSync(privateLocation2);
        this._clientPublicKey = fs.readFileSync(publicLocation2);
    }

    private loginMongo() {
        if(!this.config.db.ip || !this.config.db.port || !this.config.db.dbName) {
            this.logger.error("Invalid config.")
            this.logger.error("Missing required fields db.ip OR db.port OR db.dbName.");
            process.exit(1);
        }

        var url: string = "mongodb://" + this.config.db.ip + ":" + this.config.db.port + "/" + this.config.db.dbName;

        mongo.MongoClient.connect(url, (err, db) => {
            if(err) {
                console.log(err);
                this.logger.error("FAILED TO CONNECT TO MONGODB DATABASE!");
                process.exit(1);
            }
            this.logger.notice("Connected to MongoDB at: " + url);
            this.db = db;
        });
    }

    private setupLogger() {
        this.logger = new winston.Logger({
            transports: [
                new winston.transports.Console({colorize: true})
            ],
            levels: {
                debug: 0,
                info: 1,
                notice: 2,
                warn: 3,
                error: 4
            },
            level: "error"
        });

        winston.addColors({
            debug: "white",
            info: "green",
            notice: "blue",
            warn: "yellow",
            error: "red"
        });
    }

    private setupInfoObject() {
        this.info = {
            software: SOFTWARE,
            version: SOFTWARE_VERSION,
            apiMajor: API_VERSION_MAJOR,
            apiMinor: API_VERSION_MINOR
        };

        if(this.config.network.sendSystemData) {
            this.info.system = {
                hostname: os.hostname,
                os: os.platform(),
                osver: os.release(),
                arch: os.arch(),
                cpuCount: os.cpus().length,
                cpu: os.cpus()[0].model
            };
        }
    }

    // Server Methods ------------------------------------------------------------------------------------------------------

    constructor() {
        setupConsole(this);

        this.setupLogger();
        this.loadConfig();
        this.loadKeys();
        this.loginMongo();
        this.setupInfoObject();

        this._clients = new Map<string, client.Client>();

        this.app = express();

        this.app.use(helmet());

        this.registerGETHandle("infoRequest", this.infoRequestHandler.bind(this));

        this._authManager = new auth.AuthManager(this);
        this._clientManager = new client.ClientManager(this);
        this._fileTransferManager = new fts.FileTransferManager(this);
    }

    public registerGETHandle(path: string, func: any) {
        var path: string = "/nectar/api/"+API_VERSION_MAJOR+"/"+API_VERSION_MINOR+"/"+path;

        this.logger.info("Registering GET handle: " + path);
        this.app.get(path, func);
    }

    public run() {
        this.logger.notice("Starting on port: " + this.config.network.bindPort);

        this.appServer = this.app.listen(this.config.network.bindPort);
    }

    public shutdown() {
        this.logger.notice("Shutting down...");

        this.appServer.close(); // Stop listening

        // TODO: Complete any other shutdown operations

        this.db.close(true, () => {
            process.exit(0);
        }); // Close MongoDB connection
    }

    // Included handlers ------------------------------------------------------------------------------------------

    private infoRequestHandler(request: express.Request, response: express.Response) {
        response.status(200).send(this.info); // 200: OK
    }

    // Getters & Setters -------------------------------------------------------------------------------------------

    get serverPrivateKey(): string{
        return this._serverPrivateKey;
    }

    get serverPublicKey(): string {
        return this._serverPublicKey;
    }

    get clientPrivateKey(): string {
        return this._clientPrivateKey;
    }

    get clientPublicKey(): string {
        return this._clientPublicKey;
    }

    get database(): mongo.Db {
        return this.db;
    }

    get clients(): Map<string, client.Client> {
        return this._clients;
    }

    get authManager(): auth.AuthManager {
        return this._authManager;
    }

    get clientManager(): client.ClientManager {
        return this._clientManager;
    }

    get fileTransferManager(): fts.FileTransferManager {
        return this._fileTransferManager;
    }
}
