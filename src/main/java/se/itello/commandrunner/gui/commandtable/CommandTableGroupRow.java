package se.itello.commandrunner.gui.commandtable;

import javafx.beans.property.SimpleStringProperty;

public class CommandTableGroupRow extends CommandTableRow {
    public CommandTableGroupRow(String name, String directory, String comment) {
        super(new SimpleStringProperty(name), new SimpleStringProperty(directory), new SimpleStringProperty(comment));
    }
}
