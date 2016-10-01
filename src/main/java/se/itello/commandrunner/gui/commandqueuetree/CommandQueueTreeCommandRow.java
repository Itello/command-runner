package se.itello.commandrunner.gui.commandqueuetree;

import javafx.beans.property.SimpleStringProperty;
import se.itello.commandrunner.Command;
import se.itello.commandrunner.CommandQueue;

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
