package CommandRunner.gui.commandtable;

import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.util.converter.DefaultStringConverter;

class CommandTableCell extends TextFieldTreeTableCell<CommandTableRow, String> {
    CommandTableCell() {
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

    @Override
    public void commitEdit(String newValue) {
        super.commitEdit(newValue);

        //HACK: javafx is stupid
        getTableColumn().getTreeTableView().requestFocus();
    }
}
