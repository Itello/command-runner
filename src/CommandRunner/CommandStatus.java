package CommandRunner;

import java.util.List;

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

    static void sortCommandStatuses(List<CommandStatus> commandStatusList) {
        commandStatusList.sort((s1, s2) -> {
            boolean firstRunning = s1.equals(RUNNING);
            boolean secondRunning = s2.equals(RUNNING);
            boolean firstFail = s1.equals(FAIL);
            boolean firstIdle = s1.equals(IDLE);
            boolean secondFail = s2.equals(FAIL);
            boolean secondIdle = s2.equals(IDLE);
            if (firstRunning && !secondRunning) {
                return -1;
            } else if (secondRunning && !firstRunning) {
                return 1;
            } else if (firstFail && !secondFail) {
                return -1;
            } else if (secondFail && !firstFail) {
                return 1;
            } else if (firstIdle && !secondIdle) {
                return -1;
            } else if (secondIdle && !firstIdle) {
                return 1;
            }

            return 0;
        });
    }
}
