package CommandRunner.gui.commandtable;

import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.util.converter.DefaultStringConverter;

public class CommandTableCell extends TextFieldTreeTableCell<CommandTableRow, String> {
    public CommandTableCell() {
        super(new DefaultStringConverter());
    }

    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (!empty) {
            setText(item);
            setTooltip(new Tooltip(item));
        } else {
            setText(null);
            setTooltip(null);
        }
    }
}
