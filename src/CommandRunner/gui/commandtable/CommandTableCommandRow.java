package CommandRunner.gui.commandtable;

import CommandRunner.Command;
import CommandRunner.gui.CommandStatus;
import javafx.beans.property.SimpleStringProperty;

public class CommandTableCommandRow extends CommandTableRow {

    private final Command command;

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

    @Override
    public void setCommandComment(String commandComment) {
        super.setCommandComment(commandComment);
        command.setCommandComment(commandComment);
    }
}
