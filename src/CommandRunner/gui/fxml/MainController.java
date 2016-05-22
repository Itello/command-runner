package CommandRunner.gui.fxml;

import CommandRunner.Command;
import CommandRunner.CommandRunner;
import CommandRunner.JSONFileReader;
import CommandRunner.JsonConverter;
import CommandRunner.gui.commandqueuetree.CommandQueueTreeController;
import CommandRunner.gui.commandqueuetree.CommandQueueTreeRow;
import CommandRunner.gui.commandtable.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static CommandRunner.gui.commandtable.CommandTableRowTreeItemListManipulator.getFlatTreeItemList;

@SuppressWarnings("UnusedDeclaration")
public class MainController implements Initializable {

    @FXML
    public TreeView<CommandQueueTreeRow> commandQueueTreeView;

    @FXML
    private TreeTableView<CommandTableRow> commandTable;

    @FXML
    private TreeTableColumn<CommandTableRow, String> commandColumn;

    @FXML
    private TreeTableColumn<CommandTableRow, String> statusColumn;

    @FXML
    private TreeTableColumn<CommandTableRow, String> directoryColumn;

    @FXML
    private TreeTableColumn<CommandTableRow, String> commentColumn;

    @FXML
    private MenuBar menuBar;

    @FXML
    private TextArea commandOutputArea;

    private int dragStartIndex;

    private static final Image FOLDER_ICON = new Image("png/folder.png");
    private static final Image COMMAND_ICON = new Image("png/command.png");

    // todo: don't need this when changes are based on file load vs current
    private int changesSinceLastSave = 0;

    private CommandQueueTreeController commandQueueTreeController;

    private CommandTableController commandTableController;

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        assert commandTable != null : "fx:id=\"commandTable\" was not injected: check FXML file 'main .fxml'.";

        // todo move all Table stuff to new class in commandTable package
        commandTable.addEventFilter(KeyEvent.KEY_PRESSED, this::tableKeyPressed);
        commandTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        commandColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().commandNameAndArgumentsProperty());

        statusColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().commandStatusProperty());
        commentColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().commandCommentProperty());
        directoryColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().commandDirectoryProperty());

        setToolTipLabel(directoryColumn, "Starting directory (NOT command location)");
        setToolTipLabel(commandColumn, "Command name and arguments, including path if command is not in path");

        directoryColumn.setOnEditCommit(event -> {
            event.getTreeTablePosition().getTreeItem().getValue().setCommandDirectory(event.getNewValue());
            changesSinceLastSave++;
        });
        commentColumn.setOnEditCommit(event -> {
            event.getTreeTablePosition().getTreeItem().getValue().setCommandComment(event.getNewValue());
            changesSinceLastSave++;
        });
        commandColumn.setOnEditCommit(event -> {
            event.getTreeTablePosition().getTreeItem().getValue().setCommandNameAndArguments(event.getNewValue());
            changesSinceLastSave++;
        });

        directoryColumn.setCellFactory(param -> getTooltipTextFieldTreeTableCell());
        commentColumn.setCellFactory(param -> getTooltipTextFieldTreeTableCell());
        commandColumn.setCellFactory(param -> {
                    TreeTableCell<CommandTableRow, String> cell = getTooltipTextFieldTreeTableCell();

                    // highlight drop target by changing background color:
                    cell.setOnDragEntered(event -> cell.setStyle("-fx-background-color: gold;"));
                    cell.setOnDragExited(event -> cell.setStyle(""));
                    cell.setOnDragOver(event -> event.acceptTransferModes(TransferMode.MOVE, TransferMode.COPY));

                    cell.setOnDragDropped(event -> {
                        final TreeTableRow<CommandTableRow> treeTableRow = cell.getTreeTableRow();
                        if (treeTableRow == null) {
                            return;
                        }

                        final Dragboard dragboard = event.getDragboard();

                        if (isExternalSource(event) && dragboard.hasFiles()) {
                            String filePath = null;
                            for (File file : dragboard.getFiles()) {
                                try {
                                    final String jsonFromFile = JSONFileReader.readJsonObjectFromFile(file);
                                    if (jsonFromFile.startsWith("[")) {
                                        JSONArray array = JsonConverter.convertFromJSONToArray(jsonFromFile);
                                        final List<TreeItem<CommandTableRow>> nodes = JSONFileReader.createNodes(array);
                                        moveRowsToItem(cell.getTreeTableRow().getTreeItem(), true, true, nodes);
                                    } else {
                                        JSONObject object = JsonConverter.convertFromJSONToObject(jsonFromFile);
                                        final TreeItem<CommandTableRow> node = JSONFileReader.convertToNode(object);
                                        moveRowsToItem(cell.getTreeTableRow().getTreeItem(), true, true, Collections.singletonList(node));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
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
                                copyRowsToItem(cell.getTreeTableRow().getTreeItem());
                            } else {
                                moveRowsToItem(cell.getTreeTableRow().getTreeItem(), true, null);
                            }
                        }

                        event.setDropCompleted(true);
                        event.consume();
                    });

                    cell.setOnDragDetected(event -> {
                        // drag was detected, start drag-and-drop gesture
                        ObservableList<TreeItem<CommandTableRow>> selected = getSelectedItems();

                        if (selected != null && !selected.isEmpty()) {
                            Dragboard db = commandTable.startDragAndDrop(TransferMode.ANY);
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

                            content.put(DataFormat.PLAIN_TEXT, "drag");
                            db.setContent(content);
                            dragStartIndex = cell.getIndex();

                            event.consume();
                        }
                    });

                    return cell;
                }
        );

//        changeToAddCommandPanel(null);
//        showProgressPane(false);

        commandQueueTreeController = new CommandQueueTreeController(commandQueueTreeView, commandOutputArea);

        CommandRunner.getInstance().controllerLoaded(this);
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

    private void moveRowsToItem(TreeItem<CommandTableRow> itemToDragTo, boolean moveIntoIfGroup, List<TreeItem<CommandTableRow>> dragRows) {
        moveRowsToItem(itemToDragTo, moveIntoIfGroup, false, dragRows);
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
        if (dragRows == null) {
            dragRows = getSelectedItems();
        }

        final List<TreeItem<CommandTableRow>> rowsToCopyOrMove;
        if (copy) {
            rowsToCopyOrMove = deepCopyRows(dragRows);
        } else {
            rowsToCopyOrMove = new ArrayList<>(dragRows);
            rowsToCopyOrMove.forEach(row -> row.getParent().getChildren().remove(row));
        }

        while (index > parent.getChildren().size()) {
            index--;
        }

        if (index < 0) {
            index = 0;
        }
        parent.getChildren().addAll(index, rowsToCopyOrMove);
        final int indexToSelect = commandTable.getRow(rowsToCopyOrMove.get(0));
        Platform.runLater(() -> {
            parent.setExpanded(true);
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < rowsToCopyOrMove.size(); i++) {
                indices.add(indexToSelect + i);
            }
            selectIndices(indices);
        });

//        changesSinceLastSave++;
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

    private TreeTableCell<CommandTableRow, String> getTooltipTextFieldTreeTableCell() {
        return new CommandTableCell();
    }

    @FXML
    private void addSelectedItemsToGroup(Event event) {
        addSelectedItemsToGroup("Group");
    }

    private void addSelectedItemsToGroup(String name) {
        final List<TreeItem<CommandTableRow>> selectedItems = new ArrayList<>(getSelectedItems());
        if (selectedItems.isEmpty()) {
            return;
        }

        final TreeItem<CommandTableRow> item = selectedItems.get(0);
        final TreeItem<CommandTableRow> parent = item.getParent();
        final TreeItem<CommandTableRow> group = copyTreeItem(new TreeItem<>(new CommandTableGroupRow(name, "", "")));

        int moveToIndex = parent.getChildren().indexOf(item);

        selectedItems.forEach(selectedItem -> selectedItem.getParent().getChildren().remove(selectedItem));
        selectedItems.forEach(selectedItem -> group.getChildren().add(selectedItem));
        while (moveToIndex >= parent.getChildren().size()) {
            moveToIndex--;
        }

        if (moveToIndex < 0) {
            moveToIndex = 0;
        }

        commandTable.getSelectionModel().clearSelection();
        parent.getChildren().add(moveToIndex, group);
        editRow(commandTable.getRow(group));

        changesSinceLastSave++;
    }

    @FXML
    private void removeCommandTableRow(Event event) {
        final ArrayList<TreeItem<CommandTableRow>> commandTableRows = new ArrayList<>(getSelectedItems());

        getSelectedItems().stream()
                .filter(item -> commandTableRows.contains(item.getParent()))
                .forEach(commandTableRows::remove);

        for (final TreeItem<CommandTableRow> item : commandTableRows) {
            final CommandTableRow row = item.getValue();
            if (row instanceof CommandTableGroupRow
                    && CommandRunner.getInstance().getSettings().getConfirmNonemptyDelete()
                    && !item.getChildren().isEmpty()) {
                final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Delete non-empty group \"" + row.commandNameAndArgumentsProperty().get() + "\"?");
                alert.setHeaderText(row.commandNameAndArgumentsProperty().get() + " has children.");
                alert.setContentText("All the children will be deleted as well as the group. Are you sure you want to delete them?");

                final ButtonType buttonTypeOK = new ButtonType("OK");
                final ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

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
        changesSinceLastSave++;
    }

    @FXML
    private void addCommandTableRow(ActionEvent event) {
        TreeItem<CommandTableRow> treeItem = new TreeItem<>(
                new CommandTableCommandRow(
                        new Command("", "", "")
                )
        );


        commandTable.getRoot().getChildren().add(treeItem);
        fixGraphic(treeItem);
        editRow(commandTable.getRow(treeItem));
        changesSinceLastSave++;
    }

    private void editRow(int row) {
        // HACK: javafx is stupid
        sleep(10);
        Platform.runLater(() -> commandTable.edit(row, commandColumn));
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void runSelected(Event event) {
        ObservableList<TreeItem<CommandTableRow>> selectedItems = getSelectedItems();
        if (!selectedItems.isEmpty()) {
            runCommandTreeItems(selectedItems);
        }
    }

    @FXML
    private void runAll(ActionEvent event) {
        runCommandTreeItems(getFlatTreeItemList(commandTable.getRoot()));
    }

    private void runCommandTreeItems(List<TreeItem<CommandTableRow>> treeItemsToRun) {
        CommandRunner.getInstance().runCommandTreeItems(treeItemsToRun, commandQueueTreeController);
    }

    @FXML
    private void tableKeyPressed(KeyEvent event) {
        boolean consume = true;

        switch (event.getCode()) {
            case DELETE:
                removeCommandTableRow(event);
                break;
            case ENTER:
                if (commandTable.getEditingCell() == null) {
                    runSelected(event);
                } else {
                    consume = false;
                }
                break;
            case PAGE_UP:
                keyboardMoveSelectedRows(-1);
                break;
            case PAGE_DOWN:
                keyboardMoveSelectedRows(1);
                break;
            default:
                consume = false;
        }

        if (consume) {
            event.consume();
        }
    }

    private void keyboardMoveSelectedRows(int modifier) {
        ArrayList<TreeItem<CommandTableRow>> selected = new ArrayList<>(getSelectedItems());
        if (!selected.isEmpty()) {
            dragStartIndex = commandTable.getSelectionModel().getSelectedIndices().stream()
                    .filter(index -> index >= 0)
                    .min(Integer::compare)
                    .orElseThrow(() -> new IllegalStateException("umm"));
            if (dragStartIndex + modifier >= 0) {
                moveRowsToItem(commandTable.getTreeItem(dragStartIndex + modifier), false, null);
            }
        }
    }

    @FXML
    private void save(ActionEvent event) {
        save();
    }

    private void save() {
        CommandRunner.getInstance().save(getRoot());
        changesSinceLastSave = 0;
    }

    @FXML
    private void stop(ActionEvent event) {
        commandQueueTreeController.stopSelected();
    }

    @FXML
    private void kill(ActionEvent event) {
        commandQueueTreeController.killSelected();
    }

    @FXML
    private void settings(ActionEvent event) throws IOException {
        CommandRunner.getInstance().addSettingsStage();
    }

    private CommandTableCommandRow findRowForCommand(Command command) {
        return (CommandTableCommandRow) getFlatTreeItemList(commandTable.getRoot()).stream()
                .map(TreeItem::getValue)
                .filter(row -> row instanceof CommandTableCommandRow)
                .filter(row -> ((CommandTableCommandRow) row).getCommand() == command)
                .collect(Collectors.toList())
                .get(0);
    }

    private TreeItem<CommandTableRow> getRoot() {
        return commandTable.getRoot();
    }

    public void setRoot(TreeItem<CommandTableRow> commandTreeNode) {
        if (commandTreeNode == null) {
            commandTreeNode = new TreeItem<>(new CommandTableGroupRow("root", "", ""));
        }

        commandTable.setRoot(commandTreeNode);
        fixGraphicHierarchically(commandTreeNode);
        commandTable.getRoot().setExpanded(true);
        commandTable.setShowRoot(false);

        updateGroupStatuses(commandTable.getRoot());
    }

    private void updateGroupStatuses(TreeItem<CommandTableRow> node) {
        node.getChildren().forEach(this::updateGroupStatuses);

        if (node.getChildren().isEmpty()) {
            return;
        }

        String status = node.getChildren().get(0).getValue().commandStatusProperty().get();
        boolean sameStatus = !node.getChildren().stream()
                .anyMatch(child -> !child.getValue().commandStatusProperty().get().equals(status));

        if (sameStatus) {
            node.getValue().setCommandStatus(status);
        } else {
            node.getValue().setCommandStatus("");
        }
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

        fixGraphic(createdTreeItem);

        if (!treeItem.getChildren().isEmpty()) {
            treeItem.getChildren().forEach(child -> createdTreeItem.getChildren().add(copyTreeItem(child)));
        }

        return createdTreeItem;
    }

    private void fixGraphic(TreeItem<CommandTableRow> treeItem) {
        if (treeItem.getValue() instanceof CommandTableGroupRow) {
            treeItem.setGraphic(new ImageView(FOLDER_ICON));
        } else if (treeItem.getValue() instanceof CommandTableCommandRow) {
            treeItem.setGraphic(new ImageView(COMMAND_ICON));
        }
    }

    private void fixGraphicHierarchically(TreeItem<CommandTableRow> treeItem) {
        fixGraphic(treeItem);

        if (!treeItem.getChildren().isEmpty()) {
            treeItem.getChildren().forEach(this::fixGraphicHierarchically);
        }
    }

    public boolean hasChangesSinceLastSave() {
        return changesSinceLastSave != 0;
    }
}
