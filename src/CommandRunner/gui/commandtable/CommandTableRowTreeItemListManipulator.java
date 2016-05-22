package CommandRunner.gui.commandtable;

import CommandRunner.Command;
import javafx.scene.control.TreeItem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class CommandTableRowTreeItemListManipulator {
    public static List<TreeItem<CommandTableRow>> getFlatTreeItemList(TreeItem<CommandTableRow> root) {
        List<TreeItem<CommandTableRow>> items = new ArrayList<>();
        TreeItem<CommandTableRow> node = root;

        Deque<TreeItem<CommandTableRow>> rows = new ArrayDeque<>();
        rows.push(node);

        while (!rows.isEmpty()) {
            node = rows.removeFirst();
            node.getChildren().forEach(rows::addLast);
            items.add(node);
        }

        return items;
    }

    public static void addAllCommandRowsForTreeItem(TreeItem<CommandTableRow> item, List<CommandTableCommandRow> commandRows) {
        CommandTableRow row = item.getValue();

        if (row instanceof CommandTableCommandRow) {
            if (commandRows.contains(row)) {
                return;
            }

            CommandTableCommandRow commandRow = (CommandTableCommandRow) row;
            final Command command = commandRow.getCommand();
            commandRows.add(commandRow);

            TreeItem<CommandTableRow> parentItem = item.getParent();
            while (parentItem != null) {
                final String parentCommandDirectory = parentItem.getValue().commandDirectoryProperty().getValue();
                if (!parentCommandDirectory.isEmpty()) {
                    command.setParentCommandDirectory(parentCommandDirectory);
                    break;
                }

                parentItem = parentItem.getParent();
            }
        } else if (row instanceof CommandTableGroupRow) {
            item.getChildren().forEach(child -> addAllCommandRowsForTreeItem(child, commandRows));
        } else {
            throw new UnsupportedOperationException("unidentified row");
        }
    }
}
