package se.itello.commandrunner.gui.commandqueuetree;

import javafx.beans.property.StringProperty;
import javafx.scene.control.TreeCell;

class CommandQueueTreeCell extends TreeCell<CommandQueueTreeRow> {
    @Override
    public void updateItem(CommandQueueTreeRow item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            StringProperty stringProperty = item.nameProperty();
            if (stringProperty == null) {
                // hack: javafx is stupid
                if (item instanceof CommandQueueTreeCommandQueueRow) {
                    setText("{CommandQueue}");
                } else {
                    setText("{Command}");
                }
            } else {
                setText(stringProperty.getValue());
            }

            setGraphic(getTreeItem().getGraphic());
        }
    }
}


