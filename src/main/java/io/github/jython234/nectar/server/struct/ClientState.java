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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Represents the State of a
 * particular ClientSession.
 *
 * @author jython234
 */
public enum ClientState {
    /**
     * Client is online and connected to
     * the server with an active session.
     */
    ONLINE(0, "ClientState.ONLINE(0)"),
    /**
     * Client is offline, has performed a
     * full OS shutdown.
     */
    SHUTDOWN(1, "ClientState.SHUTDOWN(1)"),
    /**
     * Client has gone to sleep.
     */
    SLEEP(2, "ClientState.SLEEP(2)"),
    /**
     * Client is offline, the OS
     * is restarting. The Client will
     * reconnect once it goes online.
     */
    RESTART(3, "ClientState.RESTART(3)"),
    /**
     * Client state is unknown. This is
     * set when a Client's token has been revoked
     * while online. Once the token is renewed, the
     * state will be updated.
     */
    UNKNOWN(4, "ClientState.UNKNOWN(4)");

    private int state;
    private String string;

    ClientState(int state, String string) {
        this.state = state;
        this.string = string;
    }

    public static ClientState fromInt(int state) {
        switch(state) {
            case 0:
                return ONLINE;
            case 1:
                return SHUTDOWN;
            case 2:
                return SLEEP;
            case 3:
                return RESTART;
            case 4:
                return UNKNOWN;
            default:
                throw new IllegalArgumentException("State is invalid.");
        }
    }

    public int toInt() {
        return state;
    }

    public String toString() {
        return string;
    }
}
