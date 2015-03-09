package CommandRunner.gui;

import CommandRunner.Command;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


public class CommandTableRow {
    private final Command command;

    private final StringProperty commandNameAndArguments;
    private final StringProperty commandDirectory;
    private final StringProperty commandStatus;
    private final StringProperty commandComment;


    public CommandTableRow(Command command) {
        this.command = command;

        commandNameAndArguments = new SimpleStringProperty(command.getCommandNameAndArguments());
        commandDirectory = new SimpleStringProperty(command.getCommandDirectory());
        commandStatus = new SimpleStringProperty(CommandStatus.IDLE.getStringValue());
        commandComment = new SimpleStringProperty(command.getCommandComment());
    }

    public Command getCommand() {
        return command;
    }

    public String getCommandNameAndArguments() {
        return commandNameAndArguments.get();
    }

    public StringProperty commandNameAndArgumentsProperty() {
        return commandNameAndArguments;
    }

    public void setCommandNameAndArguments(String commandNameAndArguments) {
        this.commandNameAndArguments.set(commandNameAndArguments);
        command.setCommandNameAndArguments(commandNameAndArguments);
    }

    public String getCommandDirectory() {
        return commandDirectory.get();
    }

    public StringProperty commandDirectoryProperty() {
        return commandDirectory;
    }

    public void setCommandDirectory(String commandDirectory) {
        this.commandDirectory.set(commandDirectory);
        command.setCommandDirectory(commandDirectory);
    }

    public String getCommandStatus() {
        return commandStatus.get();
    }

    public StringProperty commandStatusProperty() {
        return commandStatus;
    }

    public void setCommandStatus(String commandStatus) {
        this.commandStatus.set(commandStatus);
    }

    public void updateCommandStatus() {
        setCommandStatus(command.getCommandStatus().getStringValue());
    }

    public String getCommandComment() {
        return commandComment.get();
    }

    public StringProperty commandCommentProperty() {
        return commandComment;
    }

    public void setCommandComment(String commandComment) {
        this.commandComment.set(commandComment);
        command.setCommandComment(commandComment);
    }
}
