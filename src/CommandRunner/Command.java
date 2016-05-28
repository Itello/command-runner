package CommandRunner;

import javafx.application.Platform;

import java.io.*;
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
    private BufferedWriter writer;

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

    public void sendInput(String input) {
        try {
            writer.write(input);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void execute() {
        BufferedReader reader = null;
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
            reader = new BufferedReader(inputStreamReader);
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            String lineRead;
            while ((lineRead = reader.readLine()) != null) {
                final String line = lineRead;
                commandListeners.forEach(listener -> listener.commandOutput(this, line));
            }

            commandStatus = CommandStatus.createCommandStatus(process.waitFor());
        } catch (Exception e) {
            Platform.runLater(
                    () -> {
                        String message = e.getMessage() == null ? "" : e.getMessage();
                        commandListeners.forEach(
                                listener -> listener.commandOutput(this, e.getClass().getSimpleName() + ":" + message)
                        );
                    }
            );
            this.commandStatus = CommandStatus.FAIL;
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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

        try {
            process.destroyForcibly();
            process.waitFor();
            if (!process.isAlive()) {
                commandStatus = CommandStatus.IDLE;
                commandListeners.forEach(listener -> listener.commandExecuted(this));
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public CommandStatus getCommandStatus() {
        return commandStatus;
    }

    public void setParentCommandDirectory(String parentCommandDirectory) {
        this.parentCommandDirectory = parentCommandDirectory;
    }
}
