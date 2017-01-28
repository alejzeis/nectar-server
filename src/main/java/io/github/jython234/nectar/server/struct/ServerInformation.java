package io.github.jython234.nectar.server.struct;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * JSON object that holds the server's information.
 *
 * @author jython234
 */
@RequiredArgsConstructor
public class ServerInformation {
    @Getter private final String software;
    @Getter private final String softwareVersion;
    @Getter private final int apiVersionMajor;
    @Getter private final int apiVersionMinor;
    @Getter private final SystemInfo systemInfo;

    @RequiredArgsConstructor
    public static class SystemInfo {
        @Getter private final String runtime;
        @Getter private final String arch;
        @Getter private final String os;
        @Getter private final int cpus;
    }
}
