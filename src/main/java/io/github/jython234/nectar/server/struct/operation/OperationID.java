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
    OPERATION_SET_HOSTNAME(21),
    OPERATION_DEPLOY_SCRIPT(30),
    OPERATION_DO_SHUTDOWN(40),
    OPERATION_DO_REBOOT(41),
    OPERATION_BROADCAST_MESSAGE(50);

    private int id;

    OperationID(int id) {
        this.id = id;
    }

    public static OperationID fromInt(int id) {
        switch(id) {
            case 0:
                return OPERATION_DO_UPDATE;
            case 1:
                return OPERATION_INSTALL_PACKAGE;
            case 2:
                return OPERATION_UPDATE_CLIENT_EXECUTABLE;
            case 20:
                return OPERATION_SET_TIMEZONE;
            case 21:
                return OPERATION_SET_HOSTNAME;
            case 30:
                return OPERATION_DEPLOY_SCRIPT;
            case 40:
                return OPERATION_DO_SHUTDOWN;
            case 41:
                return OPERATION_DO_REBOOT;
            case 50:
                return OPERATION_BROADCAST_MESSAGE;
            default:
                throw new IllegalArgumentException("Unknown Operation ID!");
        }
    }

    public int toInt() {
        return this.id;
    }
}
