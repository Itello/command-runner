package CommandRunner.gui.commandtable;

import CommandRunner.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommandTableController {
    private final TreeTableView<CommandTableRow> commandTable;
    private final TreeTableColumn<CommandTableRow, String> commandColumn;

    private int dragStartIndex;

    public CommandTableController(TreeTableView<CommandTableRow> commandTable, TreeTableColumn<CommandTableRow, String> commandColumn, TreeTableColumn<CommandTableRow, String> directoryColumn, TreeTableColumn<CommandTableRow, String> commentColumn) {
        this.commandTable = commandTable;
        this.commandColumn = commandColumn;

        commandTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        commandColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().commandNameAndArgumentsProperty());


        commentColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().commandCommentProperty());

        directoryColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().commandDirectoryProperty());

        setToolTipLabel(directoryColumn, "Starting directory (NOT command location)");
        setToolTipLabel(commandColumn, "Command name and arguments, including path if command is not in path");

        directoryColumn.setOnEditCommit(event ->
                event.getTreeTablePosition().getTreeItem().getValue().setCommandDirectory(event.getNewValue()));
        commentColumn.setOnEditCommit(event ->
                event.getTreeTablePosition().getTreeItem().getValue().setCommandComment(event.getNewValue()));
        commandColumn.setOnEditCommit(event ->
                event.getTreeTablePosition().getTreeItem().getValue().setCommandNameAndArguments(event.getNewValue()));

        directoryColumn.setCellFactory(param -> new CommandTableCell());
        commentColumn.setCellFactory(param -> new CommandTableCell());
        commandColumn.setCellFactory(param -> {
                    final CommandTableCell cell = new CommandTableIconCell();

                    // highlight drop target by changing background color:
                    cell.setOnDragEntered(event -> cell.setStyle("-fx-background-color: -fx-cellDragEnter;"));
                    cell.setOnDragExited(event -> cell.setStyle(""));
                    cell.setOnDragOver(event -> event.acceptTransferModes(TransferMode.MOVE, TransferMode.COPY));
            
                    cell.setOnDragDropped(event -> {
                        final TreeTableRow<CommandTableRow> treeTableRow = cell.getTreeTableRow();
                        if (treeTableRow == null) {
                            return;
                        }

                        final Dragboard dragboard = event.getDragboard();

                        TreeItem<CommandTableRow> toItem = cell.getTreeTableRow().getTreeItem();
                        if (isExternalSource(event) && dragboard.hasFiles()) {
                            List<File> files = dragboard.getFiles();
                            copyFilesToRows(toItem, files);
                        } else {
                            if (cell.getIndex() == dragStartIndex) {
                                return;
                            }

                            final boolean copying;
                            if (event.getAcceptedTransferMode().equals(TransferMode.COPY)) {
                                copying = true;
                            } else if (event.getAcceptedTransferMode().equals(TransferMode.MOVE)) {
                                copying = false;
                            } else {
                                return;
                            }

                            if (copying) {
                                copyRowsToItem(toItem);
                            } else {
                                moveRowsToItem(toItem, true);
                            }
                        }

                        event.setDropCompleted(true);
                        event.consume();
                    });

                    cell.setOnDragDetected(event -> {
                        // drag was detected, start drag-and-drop gesture
                        ObservableList<TreeItem<CommandTableRow>> selected = getSelectedItems();

                        if (selected != null && !selected.isEmpty()) {
                            Dragboard db = this.commandTable.startDragAndDrop(TransferMode.ANY);
                            final ClipboardContent content = createClipBoardContent(selected);

                            db.setContent(content);
                            dragStartIndex = cell.getIndex();

                            event.consume();
                        }
                    });

                    return cell;
                }

        );
    }

    private void copyFilesToRows(TreeItem<CommandTableRow> toItem, List<File> files) {
        for (File file : files) {
            try {
                final String jsonFromFile = JSONFileReader.readJsonObjectFromFile(file);
                if (jsonFromFile.startsWith("[")) {
                    JSONArray array = JsonConverter.convertFromJSONToArray(jsonFromFile);
                    final List<TreeItem<CommandTableRow>> nodes = JSONFileReader.createNodes(array);
                    moveRowsToItem(toItem, true, true, nodes);
                } else {
                    JSONObject object = JsonConverter.convertFromJSONToObject(jsonFromFile);
                    final TreeItem<CommandTableRow> node = JSONFileReader.convertToNode(object);
                    moveRowsToItem(toItem, true, true, Collections.singletonList(node));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private ObservableList<TreeItem<CommandTableRow>> getSelectedItems() {
        ObservableList<TreeItem<CommandTableRow>> selectedItems = commandTable.getSelectionModel().getSelectedItems();
        boolean containsNull = true;
        // unfortunate hack to refresh selected items
        while (containsNull) {
            containsNull = false;
            for (TreeItem<CommandTableRow> selectedItem : selectedItems) {
                if (selectedItem == null) {
                    containsNull = true;
                    //noinspection ResultOfMethodCallIgnored
                    selectedItems.toString();
                }
            }
        }

        return selectedItems;
    }

    private boolean isExternalSource(DragEvent event) {
        return event.getGestureSource() == null;
    }

    private void copyRowsToItem(TreeItem<CommandTableRow> itemToDragTo) {
        moveRowsToItem(itemToDragTo, true, true, null);
    }

    private void moveRowsToItem(TreeItem<CommandTableRow> itemToDragTo, boolean moveIntoIfGroup) {
        moveRowsToItem(itemToDragTo, moveIntoIfGroup, false, null);
    }

    private void moveRowsToItem(TreeItem<CommandTableRow> itemToDragTo, boolean moveIntoIfGroup, boolean copy, List<TreeItem<CommandTableRow>> dragRows) {
        if (itemToDragTo == null) {
            moveRowsToIndex(getRoot().getChildren().size(), copy, null, getRoot());
        } else {
            final TreeItem<CommandTableRow> parentToDragTo;
            int indexOfItemToDragTo;

            if (moveIntoIfGroup && itemToDragTo.getValue() instanceof CommandTableGroupRow) {
                parentToDragTo = itemToDragTo;
            } else {
                parentToDragTo = itemToDragTo.getParent();
                if (parentToDragTo == null) {
                    return;
                }
            }

            indexOfItemToDragTo = parentToDragTo.getChildren().indexOf(itemToDragTo);

            moveRowsToIndex(indexOfItemToDragTo, copy, dragRows, parentToDragTo);
        }
    }

    private void moveRowsToIndex(int index, boolean copy, List<TreeItem<CommandTableRow>> dragRows, TreeItem<CommandTableRow> parent) {
        int rowIndex = index;
        List<TreeItem<CommandTableRow>> rowsToDrag = dragRows;

        if (rowsToDrag == null) {
            rowsToDrag = getSelectedItems();
        }

        final List<TreeItem<CommandTableRow>> rowsToCopyOrMove;
        if (copy) {
            rowsToCopyOrMove = deepCopyRows(rowsToDrag);
        } else {
            rowsToCopyOrMove = new ArrayList<>(rowsToDrag);
            rowsToCopyOrMove.forEach(row -> row.getParent().getChildren().remove(row));
        }

        while (rowIndex > parent.getChildren().size()) {
            rowIndex--;
        }

        if (rowIndex < 0) {
            rowIndex = 0;
        }
        parent.getChildren().addAll(rowIndex, rowsToCopyOrMove);
        int indexToSelect = commandTable.getRow(rowsToCopyOrMove.get(0));
        Platform.runLater(() -> {
            parent.setExpanded(true);
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < rowsToCopyOrMove.size(); i++) {
                indices.add(indexToSelect + i);
            }
            selectIndices(indices);
        });
    }

    private void selectIndices(List<Integer> indices) {
        commandTable.getSelectionModel().clearSelection();
        if (indices.size() > 1) {
            List<Integer> subList = indices.subList(1, indices.size());
            int[] tail = subList.stream().mapToInt(i -> i).toArray();
            commandTable.getSelectionModel().selectIndices(indices.get(0), tail);
        } else {
            commandTable.getSelectionModel().select(indices.get(0));
        }
    }

    private List<TreeItem<CommandTableRow>> deepCopyRows(List<TreeItem<CommandTableRow>> dragRows) {
        return dragRows.stream()
                .map(this::copyTreeItem)
                .collect(Collectors.toList());
    }

    private void setToolTipLabel(TreeTableColumn<CommandTableRow, String> column, String tooltip) {
        Label directoryLabel = new Label(column.getText());
        directoryLabel.setTooltip(new Tooltip(tooltip));
        column.setText("");
        column.setGraphic(directoryLabel);
    }

    public void addSelectedItemsToGroup() {
        final List<TreeItem<CommandTableRow>> selectedItems = new ArrayList<>(getSelectedItems());
        if (selectedItems.isEmpty()) {
            return;
        }

        final TreeItem<CommandTableRow> item = selectedItems.get(0);
        final TreeItem<CommandTableRow> parent = item.getParent();
        final TreeItem<CommandTableRow> group = copyTreeItem(new TreeItem<>(new CommandTableGroupRow("Group", "", "")));

        int moveToIndex = parent.getChildren().indexOf(item);

        selectedItems.forEach(selectedItem -> selectedItem.getParent().getChildren().remove(selectedItem));
        selectedItems.forEach(selectedItem -> group.getChildren().add(selectedItem));
        group.setExpanded(true);
        while (moveToIndex > parent.getChildren().size()) {
            moveToIndex--;
        }

        if (moveToIndex < 0) {
            moveToIndex = 0;
        }

        commandTable.getSelectionModel().clearSelection();
        parent.getChildren().add(moveToIndex, group);
        editRow(commandTable.getRow(group), commandColumn);
    }

    public void removeSelectedCommandTableRows() {
        final ArrayList<TreeItem<CommandTableRow>> commandTableRows = new ArrayList<>(getSelectedItems());

        getSelectedItems().stream()
                .filter(item -> commandTableRows.contains(item.getParent()))
                .forEach(commandTableRows::remove);

        for (final TreeItem<CommandTableRow> item : commandTableRows) {
            final CommandTableRow row = item.getValue();
            if (row instanceof CommandTableGroupRow
                    && CommandRunner.getInstance().getProgramState().getConfirmNonemptyDelete()
                    && !item.getChildren().isEmpty()) {
                final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Delete non-empty group \"" + row.commandNameAndArgumentsProperty().get() + "\"?");
                alert.setHeaderText(row.commandNameAndArgumentsProperty().get() + " has children.");
                alert.setContentText("All the children will be deleted as well as the group. Are you sure you want to delete them?");

                final ButtonType buttonTypeOK = new ButtonType("OK");
                final ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

                CommandRunner.getInstance().setStyleSheets(alert.getDialogPane().getStylesheets());

                alert.getButtonTypes().setAll(buttonTypeOK, buttonTypeCancel);

                final Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == buttonTypeOK) {
                    removeCommandTableItem(item);
                } else {
                    return;
                }
            } else {
                removeCommandTableItem(item);
            }
        }

        Platform.runLater(() -> commandTable.getSelectionModel().clearSelection());
    }

    private void removeCommandTableItem(TreeItem<CommandTableRow> item) {
        item.getParent().getChildren().remove(item);
    }

    public void addCommandTableRow() {
        TreeItem<CommandTableRow> treeItem = new TreeItem<>(
                new CommandTableCommandRow(
                        new Command("", "", "")
                )
        );

        int selectedIndex = commandTable.getSelectionModel().getSelectedIndex();
        commandTable.getRoot().getChildren().add(selectedIndex+1, treeItem);
        editRow(commandTable.getRow(treeItem), commandColumn);
    }

    private void editRow(int row, TreeTableColumn<CommandTableRow, String> column) {
        new Thread() {
            @Override
            public void run() {
                quickHackSleep();
                Platform.runLater(() -> {
                    // if not visible
                    commandTable.scrollTo(row-5 > 0 ? row - 5 : 0);
                    commandTable.requestFocus();
                    commandTable.edit(row, column);
                });
            }
        }.start();
    }

    private void quickHackSleep() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void runSelectedInParallel(CommandQueueListener... listeners) {
        ObservableList<TreeItem<CommandTableRow>> selectedItems = getSelectedItems();
        if (!selectedItems.isEmpty()) {
            runCommandTreeItemsInParallel(selectedItems, listeners);
        }
    }

    public void runSelected(CommandQueueListener... listeners) {
        ObservableList<TreeItem<CommandTableRow>> selectedItems = getSelectedItems();
        if (!selectedItems.isEmpty()) {
            runCommandTreeItems(selectedItems, listeners);
        }
    }

    private void runCommandTreeItems(List<TreeItem<CommandTableRow>> treeItemsToRun, CommandQueueListener... listeners) {
        CommandRunner.getInstance().runCommandTreeItems(treeItemsToRun, listeners);
    }

    private void runCommandTreeItemsInParallel(List<TreeItem<CommandTableRow>> treeItemsToRun, CommandQueueListener... listeners) {
        CommandRunner.getInstance().runCommandTreeItemsInParallel(treeItemsToRun, listeners);
    }

    public void keyboardMoveSelectedRows(int modifier) {
        ArrayList<TreeItem<CommandTableRow>> selected = new ArrayList<>(getSelectedItems());
        if (!selected.isEmpty()) {
            dragStartIndex = commandTable.getSelectionModel().getSelectedIndices().stream()
                    .filter(index -> index >= 0)
                    .min(Integer::compare)
                    .orElseThrow(() -> new IllegalStateException("umm"));
            if (dragStartIndex + modifier >= 0) {
                moveRowsToItem(commandTable.getTreeItem(dragStartIndex + modifier), false);
            }
        }
    }

    public void setRoot(TreeItem<CommandTableRow> commandTreeNode) {
        commandTable.setRoot(commandTreeNode);
        commandTable.getRoot().setExpanded(true);
        commandTable.setShowRoot(false);
    }

    private TreeItem<CommandTableRow> copyTreeItem(TreeItem<CommandTableRow> treeItem) {
        final CommandTableRow row = treeItem.getValue();
        final CommandTableRow createdRow;

        if (row instanceof CommandTableCommandRow) {
            createdRow = new CommandTableCommandRow(((CommandTableCommandRow) row).getCommand().copy());
        } else if (row instanceof CommandTableGroupRow) {
            createdRow = new CommandTableGroupRow(row.commandNameAndArgumentsProperty().getValue(), row.commandDirectoryProperty().getValue(), row.commandCommentProperty().getValue());
        } else {
            throw new UnsupportedOperationException("invalid command row");
        }

        TreeItem<CommandTableRow> createdTreeItem = new TreeItem<>(createdRow);

        if (!treeItem.getChildren().isEmpty()) {
            treeItem.getChildren().forEach(child -> createdTreeItem.getChildren().add(copyTreeItem(child)));
        }

        return createdTreeItem;
    }

    public TreeItem<CommandTableRow> getRoot() {
        return commandTable.getRoot();
    }

    public void copySelectedToClipBoard() {
        ObservableList<TreeItem<CommandTableRow>> selected = getSelectedItems();
        if (selected != null && !selected.isEmpty()) {
            final ClipboardContent content = createClipBoardContent(selected);
            Clipboard clipboard = Clipboard.getSystemClipboard();
            clipboard.setContent(content);
        }
    }

    private ClipboardContent createClipBoardContent(ObservableList<TreeItem<CommandTableRow>> selected) {
        final ClipboardContent content = new ClipboardContent();

        try {
            File file = File.createTempFile(".commandRunner", ".json");
            PrintWriter printWriter = new PrintWriter(file, "UTF-8");
            if (selected.size() == 1) {
                JSONObject jsonObject = JsonConverter.convertToJSON(selected.get(0));
                printWriter.print(jsonObject.toString(2));
            } else {
                JSONArray array = JsonConverter.convertToJSON(selected);
                printWriter.print(array.toString(2));
            }
            printWriter.close();
            content.putFiles(Collections.singletonList(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return content;
    }

    public void cutSelectedToClipBoard() {
        copySelectedToClipBoard();
        removeSelectedCommandTableRows();
    }

    public void pasteSelectedFromClipBoard() {
        Clipboard clipboard = Clipboard.getSystemClipboard();

        List<File> files = clipboard.getFiles();
        TreeItem<CommandTableRow> toItem;
        ObservableList<TreeItem<CommandTableRow>> selectedItems = getSelectedItems();
        if (selectedItems != null && !selectedItems.isEmpty()) {
            toItem = selectedItems.get(0);
        } else {
            toItem = getRoot();
        }

        if (files != null && !files.isEmpty()) {
            copyFilesToRows(toItem, files);
        }
    }

    public boolean editSelected(TreeTableColumn<CommandTableRow, String> column) {
        if (getSelectedItems().size() == 1) {
            editRow(commandTable.getRow(getSelectedItems().get(0)), column);
            return true;
        }

        return false;
    }
}
