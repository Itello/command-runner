package se.itello.commandrunner.gui.commandqueuetree;

import javafx.beans.property.SimpleStringProperty;
import se.itello.commandrunner.CommandQueue;

class CommandQueueTreeCommandQueueRow extends CommandQueueTreeRow {
    private final CommandQueue commandQueue;

    CommandQueueTreeCommandQueueRow(CommandQueue commandQueue, String name) {
        super(new SimpleStringProperty(name));
        this.commandQueue = commandQueue;
    }

    CommandQueue getCommandQueue() {
        return commandQueue;
    }
}
