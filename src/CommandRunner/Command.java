package CommandRunner;

import CommandRunner.gui.CommandStatus;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Command implements Serializable {

    private String commandNameAndArguments;
    private String commandDirectory;
    private String commandComment;
    private Process process;
    private CommandStatus commandStatus;

    private final List<CommandListener> commandListeners;

    public Command(String commandDirectory, String commandNameAndArguments, String comment) {
        this.commandNameAndArguments = commandNameAndArguments;
        this.commandDirectory = commandDirectory;
        this.commandComment = comment;
        commandListeners = new ArrayList<>();
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

    public void execute() {
        try {
            ProcessBuilder builder = new ProcessBuilder(commandNameAndArguments.split(" "))
                    .redirectErrorStream(true);
            if (getCommandDirectory() != null && !getCommandDirectory().isEmpty()) {
                builder.directory(new File(commandDirectory));
            }
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
            e.printStackTrace();
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
        // Does not always kill. Need JNA for that.
        process.destroyForcibly();
    }

    public CommandStatus getCommandStatus() {
        return commandStatus;
    }

    public void setCommandStatus(CommandStatus commandStatus) {
        this.commandStatus = commandStatus;
    }
}
