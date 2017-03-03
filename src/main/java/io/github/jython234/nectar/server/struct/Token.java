package io.github.jython234.nectar.server.struct;

import org.json.simple.JSONObject;

/**
 * Base interface for a Token struct.
 *
 * @author jython234
 */
public interface Token {
    JSONObject constructJSON();
}
