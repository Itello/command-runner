package CommandRunner.gui.fxml;

import CommandRunner.*;
import CommandRunner.gui.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.util.converter.DefaultStringConverter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("UnusedDeclaration")
public class GUIController implements Initializable, CommandQueueListener, CommandListener {

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
    private FlowPane addCommandPane;

    @FXML
    private TextArea commandOutputArea;

    @FXML
    private GridPane progressPane;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label commandsRunningLabel;

    private CommandController activeCommandController;

    private CommandQueue commandQueue;

    private int commandsToRun;

    private Command lastCommandStarted;

    private int dragStartIndex;

    private final Image folderIcon = new Image("png/folder.png");

    private int changesSinceLastSave = 0;

    @FXML
    private Button addButton;

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        assert commandTable != null : "fx:id=\"commandTable\" was not injected: check FXML file 'gui.fxml'.";

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

        commandQueue = new CommandQueue(this);
        changeToAddCommandPanel(null);
        showProgressPane(false);

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

    private TextFieldTreeTableCell<CommandTableRow, String> getTooltipTextFieldTreeTableCell() {
        return new TextFieldTreeTableCell<CommandTableRow, String>(new DefaultStringConverter()) {

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    setTooltip(new Tooltip(item));
                }
            }
        };
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
        final TreeItem<CommandTableRow> group = copyTreeItem(new TreeItem<>(new CommandTableGroupRow(name)));

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

        commandTable.edit(commandTable.getRow(group), commandColumn);
        commandTable.focusModelProperty().get().focus(commandTable.getRow(group), commandColumn);

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
    private void changeToAddCommandPanel(ActionEvent event) {
        changeAddCommandPanelTo(CommandRunner.getInstance().getCommandFXML());
    }

    private void changeAddCommandPanelTo(FXMLLoader loader) {
        try {
            addCommandPane.getChildren().setAll((Parent) loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
        activeCommandController = loader.getController();
    }

    @FXML
    private void addCommandTableRow(ActionEvent event) {
        commandTable.getRoot().getChildren().add(
                new TreeItem<>(
                        new CommandTableCommandRow(
                                new Command(
                                        activeCommandController.getCommandDirectory(),
                                        activeCommandController.getCommandNameAndArguments(),
                                        ""
                                )
                        ))
        );
        changesSinceLastSave++;
    }

    @FXML
    private void runSelected(Event event) {
        runCommandTreeItems(getSelectedItems());
    }

    @FXML
    private void runAll(ActionEvent event) {
        runCommandTreeItems(getFlatTreeItemList(commandTable.getRoot()));
    }

    public void runAllCommandsWithComment(String comment, TreeItem<CommandTableRow> root) {
        if (comment == null || comment.equals("")) {
            return;
        }

        final List<TreeItem<CommandTableRow>> all = getFlatTreeItemList(root);
        final List<TreeItem<CommandTableRow>> commandsWithComment = new ArrayList<>();
        for (final TreeItem<CommandTableRow> commandItem : all) {
            if (commandItem.getValue().commandCommentProperty().getValue().equals(comment)) {
                commandsWithComment.add(commandItem);
            }
        }

        runCommandTreeItems(commandsWithComment);
    }

    private void runCommandTreeItems(List<TreeItem<CommandTableRow>> treeItemsToRun) {
        List<CommandTableCommandRow> commandTableRowsToRun = new ArrayList<>();
        treeItemsToRun.forEach(item -> addAllCommandRowsForTreeItem(item, commandTableRowsToRun));
        if (commandQueue == null) {
            commandQueue = new CommandQueue(CommandRunner.getInstance());
        }

        commandQueue.setCommands(
                commandTableRowsToRun.stream()
                        .map(CommandTableCommandRow::getCommand)
                        .collect(Collectors.toList())
        );

        if (commandTable != null) {
            commandTable.getSelectionModel().clearSelection();
        }

        commandQueue.start();
    }

    private void addAllCommandRowsForTreeItem(TreeItem<CommandTableRow> item, List<CommandTableCommandRow> commandRows) {
        CommandTableRow row = item.getValue();

        if (row instanceof CommandTableCommandRow) {
            if (commandRows.contains(row)) {
                return;
            }

            CommandTableCommandRow commandRow = (CommandTableCommandRow) row;
            final Command command = commandRow.getCommand();
            command.setCommandStatus(CommandStatus.IDLE);
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

    private void showProgressPane(boolean show) {
        addCommandPane.setVisible(!show);
        progressPane.setVisible(show);
        commandTable.setDisable(show);
        addButton.setDisable(show);
        addButton.setVisible(!show);
        menuBar.setDisable(show);
    }

    @Override
    public void commandQueueStarted(int items) {
        commandOutputArea.clear();
        showProgressPane(true);
        commandsToRun = items;
    }

    @Override
    public void commandQueueFinished() {
        showProgressPane(false);
        changeToAddCommandPanel(null);
    }

    @Override
    public void commandQueueIsProcessing(Command command, int itemsLeft) {
        command.setCommandStatus(CommandStatus.RUNNING);
        findRowForCommand(command).updateCommandStatus();
        progressBar.setProgress((double) (commandsToRun - itemsLeft + 1) / (double) commandsToRun);
        commandsRunningLabel.setText("Running (" + (commandsToRun - itemsLeft + 1) + "/" + commandsToRun + ")");
        lastCommandStarted = command;
        command.addCommandListener(this);
        commandOutputArea.appendText("-------  executing... " + command.getCommandNameAndArguments() + "  -------\n");
    }

    @Override
    public void commandExecuted(Command command) {
        findRowForCommand(command).updateCommandStatus();
        updateGroupStatuses(commandTable.getRoot());
        commandOutputArea.appendText("\n");
    }

    @Override
    public void commandOutput(Command command, String text) {
        commandOutputArea.appendText(text + "\n");
    }

    @FXML
    private void save(ActionEvent event) {
        save();
    }

    public void save() {
        CommandRunner.getInstance().save(getRoot());
        changesSinceLastSave = 0;
    }

    @FXML
    private void stop(ActionEvent event) {
        commandQueue.stopWhenCurrentCommandFinishes();
    }

    @FXML
    private void kill(ActionEvent event) {
        commandQueue.kill();
        findRowForCommand(lastCommandStarted).updateCommandStatus();
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

    private List<TreeItem<CommandTableRow>> getFlatTreeItemList(TreeItem<CommandTableRow> root) {
        List<TreeItem<CommandTableRow>> items = new ArrayList<>();
        TreeItem<CommandTableRow> node = root;

        Deque<TreeItem<CommandTableRow>> rows = new ArrayDeque<>();
        rows.push(node);

        while (!rows.isEmpty()) {
            node = rows.removeFirst();
            final TreeItem<CommandTableRow> parent = node;
            node.getChildren().forEach(rows::addLast);
            items.add(node);
        }

        return items;
    }

    private TreeItem<CommandTableRow> getRoot() {
        return commandTable.getRoot();
    }

    public void setRoot(TreeItem<CommandTableRow> commandTreeNode) {
        TreeItem<CommandTableRow> commandTreeNode1 = commandTreeNode;
        if (commandTreeNode1 == null) {
            commandTreeNode1 = new TreeItem<>(new CommandTableGroupRow("root"));
        }

        commandTable.setRoot(copyTreeItem(commandTreeNode1));
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
            createdRow = new CommandTableGroupRow(row.commandNameAndArgumentsProperty().getValue());
        } else {
            throw new UnsupportedOperationException("invalid command row");
        }

        TreeItem<CommandTableRow> createdTreeItem = new TreeItem<>(createdRow);

        if (createdRow instanceof CommandTableGroupRow) {
            createdTreeItem.setGraphic(new ImageView(folderIcon));
        }

        if (!treeItem.getChildren().isEmpty()) {
            treeItem.getChildren().forEach(child -> createdTreeItem.getChildren().add(copyTreeItem(child)));
        }

        return createdTreeItem;
    }

    public boolean hasChangesSinceLastSave() {
        return changesSinceLastSave != 0;
    }
}
