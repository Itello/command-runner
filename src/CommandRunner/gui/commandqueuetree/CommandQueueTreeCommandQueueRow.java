package CommandRunner.gui.commandqueuetree;

import CommandRunner.CommandQueue;
import javafx.beans.property.SimpleStringProperty;

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
