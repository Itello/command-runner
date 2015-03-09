package CommandRunner.gui;

public enum CommandStatus {
    IDLE,
    OK,
    RUNNING,
    FAIL;

    public static CommandStatus createCommandStatus(int value) {
        switch (value) {
            case -2:
                return RUNNING;
            case -1:
                return IDLE;
            case 0:
                return OK;
            default:
                return FAIL;
        }
    }

    public String getStringValue() {
        switch (this) {
            case RUNNING:
                return "running";
            case IDLE:
                return "idle";
            case OK:
                return "ok";
            case FAIL:
                return "fail";
            default:
                return "?";
        }
    }
}
