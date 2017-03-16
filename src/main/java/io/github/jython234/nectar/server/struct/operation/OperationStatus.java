package io.github.jython234.nectar.server.struct.operation;

/**
 * Represents the status of a Client which is capable of
 * processing operations.
 *
 * @author jython234
 */
public enum OperationStatus {
    IDLE(0),
    IN_PROGRESS(1),
    SUCCESS(2),
    FAILED(3);

    private int status;

    OperationStatus(int status) {
        this.status = status;
    }

    public static OperationStatus fromInt(int status) {
        switch(status) {
            case 0:
                return IDLE;
            case 1:
                return IN_PROGRESS;
            case 2:
                return SUCCESS;
            case 3:
                return FAILED;
            default:
                throw new RuntimeException("Unknown status!");
        }
    }

    public int toInt() {
        return this.status;
    }
}
