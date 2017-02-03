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
