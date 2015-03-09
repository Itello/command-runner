package CommandRunner.gui;

import CommandRunner.Command;
import javafx.beans.property.SimpleStringProperty;

public class CommandTableCommandRow extends CommandTableRow {

    private Command command;

    public CommandTableCommandRow(Command command) {
        super(
                new SimpleStringProperty(command.getCommandNameAndArguments()),
                new SimpleStringProperty(command.getCommandDirectory()),
                new SimpleStringProperty(CommandStatus.IDLE.getStringValue()),
                new SimpleStringProperty(command.getCommandComment())
        );

        this.command = command;
    }

    public Command getCommand() {
        return command;
    }

    @Override
    public void setCommandNameAndArguments(String commandNameAndArguments) {
        super.setCommandNameAndArguments(commandNameAndArguments);
        command.setCommandNameAndArguments(commandNameAndArguments);
    }

    @Override
    public void setCommandDirectory(String commandDirectory) {
        super.setCommandDirectory(commandDirectory);
        command.setCommandDirectory(commandDirectory);
    }

    public void updateCommandStatus() {
        setCommandStatus(command.getCommandStatus().getStringValue());
    }

    @Override
    public void setCommandComment(String commandComment) {
        super.setCommandComment(commandComment);
        command.setCommandComment(commandComment);
    }
}
