package CommandRunner.gui.commandtable;

import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class CommandTableIconCell extends CommandTableCell {
    private static final Image FOLDER_ICON = new Image("png/folder.png");
    private static final Image COMMAND_ICON = new Image("png/command.png");

    CommandTableIconCell() {
        super();
    }

    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (!empty) {
            final TreeItem<CommandTableRow> treeItem = getTreeTableRow().getTreeItem();
            Image icon = null;
            if (treeItem != null) {
                CommandTableRow row = treeItem.getValue();
                if (row instanceof CommandTableGroupRow) {
                    icon = FOLDER_ICON;
                } else if (row instanceof CommandTableCommandRow) {
                    icon = COMMAND_ICON;
                }
            }

            if (icon != null) {
                setGraphic(new ImageView(icon));
            }
        } else {
            setGraphic(null);
        }
    }
}
