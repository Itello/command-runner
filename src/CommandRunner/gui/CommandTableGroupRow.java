package CommandRunner.gui;

import javafx.beans.property.SimpleStringProperty;

public class CommandTableGroupRow extends CommandTableRow {
    public CommandTableGroupRow(String name) {
        super(new SimpleStringProperty(name), new SimpleStringProperty(""), new SimpleStringProperty(""), new SimpleStringProperty(""));
    }
}
