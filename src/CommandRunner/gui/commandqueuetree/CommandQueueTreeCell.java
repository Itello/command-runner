package CommandRunner.gui.commandqueuetree;

import javafx.scene.control.TreeCell;

class CommandQueueTreeCell extends TreeCell<CommandQueueTreeRow> {
    @Override
    public void updateItem(CommandQueueTreeRow item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            setText(item.nameProperty().getValue());
            setGraphic(getTreeItem().getGraphic());
        }
    }
}


