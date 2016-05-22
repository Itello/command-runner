package CommandRunner.gui.commandqueuetree;

import CommandRunner.Command;
import CommandRunner.CommandQueue;
import javafx.beans.property.SimpleStringProperty;

class CommandQueueTreeCommandRow extends CommandQueueTreeRow {
    private final Command command;
    private final CommandQueue commandQueue;

    CommandQueueTreeCommandRow(Command command, CommandQueue commandQueue) {
        super(new SimpleStringProperty(command.getCommandNameAndArguments()));
        this.command = command;
        this.commandQueue = commandQueue;
    }

    public Command getCommand() {
        return command;
    }

    CommandQueue getCommandQueue() {
        return commandQueue;
    }
}
