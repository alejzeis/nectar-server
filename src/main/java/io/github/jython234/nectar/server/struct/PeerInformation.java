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
package io.github.jython234.nectar.server.struct;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * JSON object that holds the server's information.
 *
 * @author jython234
 */
@RequiredArgsConstructor
public class PeerInformation {
    @Getter private final String software;
    @Getter private final String softwareVersion;
    @Getter private final int apiVersionMajor;
    @Getter private final int apiVersionMinor;
    @Getter private final String serverID;
    @Getter private final String hostname;
    @Getter private final SystemInfo systemInfo;

    public static PeerInformation parseFromJSON(JSONObject obj) {
        return new PeerInformation(
                (String) obj.get("software"),
                (String) obj.get("softwareVersion"),
                ((Long) obj.get("apiVersionMajor")).intValue(),
                ((Long) obj.get("apiVersionMinor")).intValue(),
                (String) obj.get("serverID"),
                (String) obj.getOrDefault("hostname", "!UNKNOWN"),
                SystemInfo.parseFromJSON((JSONObject) obj.get("systemInfo"))
        );
    }

    public static PeerInformation parseFromJSON(String json) throws ParseException {
        JSONParser parser = new JSONParser();
        return PeerInformation.parseFromJSON((JSONObject) parser.parse(json));
    }

    @SuppressWarnings("unchecked")
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();

        obj.put("software", software);
        obj.put("softwareVersion", softwareVersion);
        obj.put("apiVersionMajor", apiVersionMajor);
        obj.put("apiVersionMinor", apiVersionMinor);
        obj.put("serverID", serverID);
        obj.put("hostname", hostname);
        obj.put("systemInfo", systemInfo.toJSON());

        return obj;
    }

    @RequiredArgsConstructor
    public static class SystemInfo {
        @Getter private final String runtime;
        @Getter private final String arch;
        @Getter private final String os;
        @Getter private final String osVersion;
        @Getter private final String cpu;
        @Getter private final int cpus;

        public static SystemInfo parseFromJSON(JSONObject obj) {
            return new SystemInfo(
                    (String) obj.get("runtime"),
                    (String) obj.get("arch"),
                    (String) obj.get("os"),
                    (String) obj.get("osVersion"),
                    (String) obj.get("cpu"),
                    ((Long) obj.get("cpus")).intValue()
            );
        }

        @SuppressWarnings("unchecked")
        public JSONObject toJSON() {
            JSONObject obj = new JSONObject();

            obj.put("runtime", runtime);
            obj.put("arch", arch);
            obj.put("os", os);
            obj.put("osVerison", osVersion);
            obj.put("cpu", cpu);
            obj.put("cpus", cpus);

            return obj;
        }
    }
}
