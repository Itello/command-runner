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
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.util.converter.DefaultStringConverter;

import java.io.IOException;
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

    private List<TreeItem<CommandTableRow>> dragRows;
    private int dragStartIndex;

    private final Image folderIcon = new Image("png/folder.png");

    private int changesSinceLastSave = 0;

    @FXML
    private Button addButton;

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        assert commandTable != null : "fx:id=\"commandTable\" was not injected: check FXML file 'gui.fxml'.";

        commandTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        commandColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().commandNameAndArgumentsProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().commandStatusProperty());
        commentColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().commandCommentProperty());
        directoryColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().commandDirectoryProperty());

        setToolTipLabel(directoryColumn, "Starting directory (NOT command location)");
        setToolTipLabel(commandColumn, "Command name and arguments, including path if command is not in path");

        commandColumn.setOnEditCommit(event -> event.getTreeTablePosition().getTreeItem().getValue().setCommandNameAndArguments(event.getNewValue()));
        directoryColumn.setOnEditCommit(event -> event.getTreeTablePosition().getTreeItem().getValue().setCommandDirectory(event.getNewValue()));
        commentColumn.setOnEditCommit(event -> event.getTreeTablePosition().getTreeItem().getValue().setCommandComment(event.getNewValue()));

        directoryColumn.setCellFactory(param -> getTooltipTextFieldTreeTableCell());
        commentColumn.setCellFactory(param -> getTooltipTextFieldTreeTableCell());

        commandColumn.setCellFactory(param -> {
                    TreeTableCell<CommandTableRow, String> cell = getTooltipTextFieldTreeTableCell();

                    // highlight drop target by changing background color:
                    cell.setOnDragEntered(event -> cell.setStyle("-fx-background-color: gold;"));
                    cell.setOnDragExited(event -> cell.setStyle(""));
                    cell.setOnDragOver(event -> event.acceptTransferModes(TransferMode.MOVE));

                    cell.setOnDragDropped(event -> {
                        if (cell.getIndex() == dragStartIndex) {
                            return;
                        }

                        final TreeTableRow<CommandTableRow> treeTableRow = cell.getTreeTableRow();
                        if (treeTableRow == null) {
                            return;
                        }

                        moveRowsToItem(cell.getTreeTableRow().getTreeItem(), dragRows, true);

                        dragRows = null;
                        event.consume();
                    });

                    cell.setOnDragDetected(event -> {
                        // drag was detected, start drag-and-drop gesture
                        ObservableList<TreeItem<CommandTableRow>> selected = commandTable.getSelectionModel().getSelectedItems();
                        if (selected != null && !selected.isEmpty()) {
                            Dragboard db = commandTable.startDragAndDrop(TransferMode.ANY);
                            final ClipboardContent content = new ClipboardContent();
                            content.putString("Drag Me!");
                            db.setContent(content);
                            dragStartIndex = cell.getIndex();
                            dragRows = new ArrayList<>(selected);
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

    private void moveRowsToItem(TreeItem<CommandTableRow> itemToDragTo, List<TreeItem<CommandTableRow>> dragRows, boolean moveIntoIfGroup) {
        if (itemToDragTo == null) {
            return;
        }

        final TreeItem<CommandTableRow> parentToDragTo;
        int indexOfItemToDragTo;

        if (moveIntoIfGroup && itemToDragTo.getValue() instanceof CommandTableGroupRow) {
            parentToDragTo = itemToDragTo;
            indexOfItemToDragTo = 0;
        } else {
            parentToDragTo = itemToDragTo.getParent();
            if (parentToDragTo == null) {
                return;
            }

            indexOfItemToDragTo = parentToDragTo.getChildren().indexOf(itemToDragTo);

            while (indexOfItemToDragTo > parentToDragTo.getChildren().size()) {
                indexOfItemToDragTo--;
            }

            if (indexOfItemToDragTo < 0) {
                indexOfItemToDragTo = 0;
            }
        }

        commandTable.getSelectionModel().clearSelection();
        TreeItem<CommandTableRow> node = parentToDragTo;

        do {
            if (dragRows.contains(node)) {
                return;
            }
        } while ((node = node.getParent()) != null);


        dragRows.forEach(row -> row.getParent().getChildren().remove(row));
        parentToDragTo.getChildren().addAll(indexOfItemToDragTo, dragRows);
        int firstRowIndex = commandTable.getRow(dragRows.get(0));

        parentToDragTo.setExpanded(true);
        commandTable.getSelectionModel().clearSelection();
        Platform.runLater(() -> commandTable.getSelectionModel().selectRange(firstRowIndex, firstRowIndex + dragRows.size()));
        changesSinceLastSave++;
    }

    @FXML
    private void addSelectedItemsToGroup(Event event) {
        addSelectedItemsToGroup("Group");
    }

    private void addSelectedItemsToGroup(String name) {
        final List<TreeItem<CommandTableRow>> selectedItems = new ArrayList<>(commandTable.getSelectionModel().getSelectedItems());
        if (selectedItems.isEmpty()) {
            return;
        }

        final TreeItem<CommandTableRow> item = selectedItems.get(0);
        final TreeItem<CommandTableRow> parent = item.getParent();
        final TreeItem<CommandTableRow> group = createTreeItem(new CommandTreeNode(name, null));

        int moveToIndex = parent.getChildren().indexOf(item);

        selectedItems.forEach(selectedItem -> selectedItem.getParent().getChildren().remove(selectedItem));
        selectedItems.forEach(selectedItem -> group.getChildren().add(selectedItem));
        while (moveToIndex >= parent.getChildren().size()) {
            moveToIndex--;
        }

        if (moveToIndex < 0) {
            moveToIndex = 0;
        }

        parent.getChildren().add(moveToIndex, group);
        commandTable.getSelectionModel().clearSelection();
        Platform.runLater(() -> commandTable.edit(commandTable.getRow(group), commandColumn));

        changesSinceLastSave++;
    }

    @FXML
    private void removeCommandTableRow(Event event) {
        final ArrayList<TreeItem<CommandTableRow>> commandTableRows = new ArrayList<>(commandTable.getSelectionModel().getSelectedItems());

        commandTable.getSelectionModel().getSelectedItems().stream()
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
                if (result.get() == buttonTypeOK) {
                    removeCommandTableItem(item);
                } else {
                    return;
                }
            } else {
                removeCommandTableItem(item);
            }
        }

        commandTable.getSelectionModel().clearSelection();
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
        runCommandTreeItems(commandTable.getSelectionModel().getSelectedItems());
    }

    @FXML
    private void runAll(ActionEvent event) {
        runCommandTreeItems(getFlatTreeItemList());
    }

    private void runCommandTreeItems(List<TreeItem<CommandTableRow>> treeItemsToRun) {
        List<CommandTableCommandRow> commandRowsToRun = new ArrayList<>();
        treeItemsToRun.forEach(item -> addAllCommandRowsForTreeItem(item, commandRowsToRun));

        commandQueue.setCommands(
                commandRowsToRun.stream()
                        .map(CommandTableCommandRow::getCommand)
                        .collect(Collectors.toList())
        );
        commandTable.getSelectionModel().clearSelection();

        commandQueue.start();
    }

    private void addAllCommandRowsForTreeItem(TreeItem<CommandTableRow> item, List<CommandTableCommandRow> commandRows) {
        CommandTableRow row = item.getValue();

        if (row instanceof CommandTableCommandRow) {
            if (commandRows.contains(row)) {
                return;
            }

            CommandTableCommandRow commandRow = (CommandTableCommandRow) row;
            commandRow.getCommand().setCommandStatus(CommandStatus.IDLE);
            commandRows.add(commandRow);
        } else if (row instanceof CommandTableGroupRow) {
            item.getChildren().forEach(child -> addAllCommandRowsForTreeItem(child, commandRows));
        } else {
            throw new UnsupportedOperationException("unidentified row");
        }
    }

    @FXML
    private void tableKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case DELETE:
                removeCommandTableRow(event);
                break;
            case ENTER:
            if (commandTable.getEditingCell() == null) {
                runSelected(event);
                break;
            }
//            case UP:
//                if (event.isAltDown()) {
//                    keyboardMoveSelectedRows(-1);
//                }
//                break;
//            case DOWN:
//                if (event.isAltDown()) {
//                    keyboardMoveSelectedRows(1);
//                }
//                break;
            default:
        }
    }

    private void keyboardMoveSelectedRows(int modifier) {
        final List<TreeItem<CommandTableRow>> selected = new ArrayList<>(commandTable.getSelectionModel().getSelectedItems());
        if (!selected.isEmpty()) {
            dragStartIndex = commandTable.getRow(selected.get(0));
            moveRowsToItem(commandTable.getTreeItem(dragStartIndex + modifier), new ArrayList<>(selected), false);
        }
    }

    private void showProgressPane(boolean show) {
        addCommandPane.setVisible(!show);
        progressPane.setVisible(show);
        commandTable.setDisable(show);
        addButton.setDisable(show);
        menuBar.setDisable(show);
    }

    @Override
    public void commandQueueStarted(int items) {
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
        commandOutputArea.appendText("\n-------  executing... " + command.getCommandNameAndArguments() + "  -------\n");
    }

    @Override
    public void commandExecuted(Command command) {
        findRowForCommand(command).updateCommandStatus();
        updateGroupStatuses(commandTable.getRoot());
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
        CommandRunner.getInstance().save(getRootCommandTreeNode());
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
        return (CommandTableCommandRow) getFlatTreeItemList().stream()
                .map(TreeItem::getValue)
                .filter(row -> row instanceof CommandTableCommandRow)
                .filter(row -> ((CommandTableCommandRow) row).getCommand() == command)
                .collect(Collectors.toList())
                .get(0);
    }

    private List<TreeItem<CommandTableRow>> getFlatTreeItemList() {
        List<TreeItem<CommandTableRow>> items = new ArrayList<>();
        TreeItem<CommandTableRow> node = commandTable.getRoot();

        Deque<TreeItem<CommandTableRow>> rows = new ArrayDeque<>();
        rows.push(node);

        while (!rows.isEmpty()) {
            node = rows.removeFirst();
            node.getChildren().forEach(rows::addLast);
            items.add(node);
        }

        return items;
    }

    private CommandTreeNode getRootCommandTreeNode() {
        TreeItem<CommandTableRow> item = commandTable.getRoot();

        Deque<TreeItem<CommandTableRow>> rows = new ArrayDeque<>();
        Deque<CommandTreeNode> nodes = new ArrayDeque<>();
        rows.push(item);
        final CommandTreeNode rootNode = new CommandTreeNode(item.getValue().commandNameAndArgumentsProperty().getValue(), null);
        nodes.push(rootNode);
        CommandTreeNode node;

        while (!rows.isEmpty()) {
            item = rows.removeFirst();
            node = nodes.removeFirst();
            final CommandTreeNode parentNode = node;
            item.getChildren().forEach(child -> {

                Command command = null;
                CommandTableRow row = child.getValue();
                if (row instanceof CommandTableCommandRow) {
                    command = ((CommandTableCommandRow) row).getCommand();
                }

                final CommandTreeNode childNode = new CommandTreeNode(row.commandNameAndArgumentsProperty().getValue(), command);
                rows.addLast(child);
                nodes.addLast(childNode);
                parentNode.addChild(childNode);
            });
        }

        return rootNode;
    }

    public void setRoot(CommandTreeNode commandTreeNode) {
        CommandTreeNode commandTreeNode1 = commandTreeNode;
        if (commandTreeNode1 == null) {
            commandTreeNode1 = new CommandTreeNode("root", null);
        }

        commandTable.setRoot(createTreeItem(commandTreeNode1));
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

    private TreeItem<CommandTableRow> createTreeItem(CommandTreeNode node) {
        final CommandTableRow row;

        if (node.hasCommand()) {
            row = new CommandTableCommandRow(node.getCommand());
        } else {
            row = new CommandTableGroupRow(node.getName());
        }

        TreeItem<CommandTableRow> item = new TreeItem<>(row);

        if (!node.hasCommand()) {
            item.setGraphic(new ImageView(folderIcon));
        }

        if (node.hasChildren()) {
            node.getChildren().forEach(child -> item.getChildren().add(createTreeItem(child)));
        }

        return item;
    }

    public boolean hasChangesSinceLastSave() {
        return changesSinceLastSave != 0;
    }
}
