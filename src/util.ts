import * as fs from "fs";
import * as os from "os";
import * as ini from "ini";

const DEFAULT_CONFIG =
`; Nectar-Server Config File\n
[network]
; The port which the server will bind to and listen for requests
bindPort=8080
; If the server should send system information when requested
sendSystemData=true\n
[db]
; IP address of the MongoDB server to store data to
ip=127.0.0.1
port=27017
; The Name of the database which nectar will store it's data in:
dbName=nectar\n
[security]
; The location of the private and public ES384 keys for the SERVER relative to the config directory
serverPublicKey=keys/server-pub.pem
serverPrivateKey=keys/server.pem\n
; The location of the private and public ES384 keys for CLIENTS relative to the config directory
clientPublicKey=keys/client-pub.pem
clientPrivateKey=keys/client.pem\n
[fts]
; The root directory where all user files should be stored for the FTS (File Transfer System)
; It can be relative to the config directory or an absolute path on the filesystem.
directory=fts
`;

export function getConfigDirLocation(system: boolean = false): string {
    if(!system) {
        return process.cwd(); // Current directory
    }

    switch(os.platform()) {
        case "darwin":
        case "freebsd":
        case "linux":
        case "openbsd":
            return "/etc/nectar-server";
        default:
            // Store in the current directory by default.
            return process.cwd();
    }
}

export function getConfigFileLocation(system: boolean = false): string {
    return getConfigDirLocation(system) + "/server.ini";
}

export function loadConfig(location: string): any {
    if(!fs.existsSync(location)) {
        fs.writeFileSync(location, DEFAULT_CONFIG);
    }
    var config = ini.parse(fs.readFileSync(location, "utf-8"));
    return config;
}
