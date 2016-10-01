package se.itello.commandrunner.gui.commandqueuetree;

import javafx.beans.property.StringProperty;

public class CommandQueueTreeRow {
    private final StringProperty name;

    CommandQueueTreeRow(StringProperty name) {
        this.name = name;
    }

    StringProperty nameProperty() {
        return name;
    }
}
