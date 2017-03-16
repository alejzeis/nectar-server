package io.github.jython234.nectar.server.struct.operation;

/**
 * Represents the identifier for a specific operation.
 *
 * @author jython234
 */
public enum OperationID {
    OPERATION_DO_UPDATE(0),
    OPERATION_INSTALL_PACKAGE(1),
    OPERATION_UPDATE_CLIENT_EXECUTABLE(2),
    OPERATION_SET_TIMEZONE(20),
    OPERATION_DEPLOY_SCRIPT(30),
    OPERATION_DO_SHUTDOWN(40),
    OPERATION_DO_REBOOT(41),
    OPERATION_BROADCAST_MESSAGE(50);

    private int id;

    OperationID(int id) {
        this.id = id;
    }

    public int toInt() {
        return this.id;
    }
}
