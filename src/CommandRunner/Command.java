package CommandRunner;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Command {

    private String commandNameAndArguments;
    private String commandDirectory;
    private String parentCommandDirectory;
    private String commandComment;
    private Process process;
    private CommandStatus commandStatus;

    private final List<CommandListener> commandListeners;

    public Command(String commandDirectory, String commandNameAndArguments, String comment) {
        this.commandNameAndArguments = commandNameAndArguments;
        this.commandDirectory = commandDirectory;
        this.commandComment = comment;
        commandListeners = new ArrayList<>();
        commandStatus = CommandStatus.IDLE;
    }

    public Command copy() {
        return new Command(commandDirectory, commandNameAndArguments, commandComment);
    }

    public String getCommandComment() {
        return commandComment;
    }

    public void setCommandComment(String commandComment) {
        this.commandComment = commandComment;
    }

    public String getCommandNameAndArguments() {
        return commandNameAndArguments;
    }

    public void setCommandNameAndArguments(String commandNameAndArguments) {
        this.commandNameAndArguments = commandNameAndArguments;
    }

    public String getCommandDirectory() {
        return commandDirectory;
    }

    public void setCommandDirectory(String commandDirectory) {
        this.commandDirectory = commandDirectory;
    }

    public void addCommandListener(CommandListener listener) {
        commandListeners.add(listener);
    }

    void execute() {
        try {
            ProcessBuilder builder = new ProcessBuilder(commandNameAndArguments.split(" "))
                    .redirectErrorStream(true);
            if (getCommandDirectory() != null && !getCommandDirectory().isEmpty()) {
                builder.directory(new File(commandDirectory));
            } else if (parentCommandDirectory != null) {
                builder.directory(new File(parentCommandDirectory));
            }

            commandStatus = CommandStatus.RUNNING;
            process = builder.start();
            InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String lineRead;
            while ((lineRead = bufferedReader.readLine()) != null) {
                final String line = lineRead;
                Platform.runLater(
                        () -> commandListeners.forEach(
                                listener -> listener.commandOutput(this, line)
                        )
                );
            }

            commandStatus = CommandStatus.createCommandStatus(process.waitFor());
        } catch (Exception e) {
            Platform.runLater(
                    () -> commandListeners.forEach(
                            listener -> listener.commandOutput(this, e.getMessage())
                    )
            );
            this.commandStatus = CommandStatus.FAIL;
        } finally {
            Platform.runLater(
                    () -> {
                        commandListeners.forEach(listener -> listener.commandExecuted(this));
                        commandListeners.clear();
                    }
            );
        }
    }

    public void kill() {
        if (process == null || !process.isAlive()) {
            return;
        }

        // TODO: Does not always kill. Need JNA for that.
        process.destroyForcibly();
        if (!process.isAlive()) {
            commandStatus = CommandStatus.IDLE;
            commandListeners.forEach(listener -> listener.commandExecuted(this));
        }
    }

    public CommandStatus getCommandStatus() {
        return commandStatus;
    }

    public void setParentCommandDirectory(String parentCommandDirectory) {
        this.parentCommandDirectory = parentCommandDirectory;
    }
}
