package se.itello.commandrunner;

public interface CommandListener {
    void commandExecuted(Command command);
    void commandOutput(Command command, String text);
}
