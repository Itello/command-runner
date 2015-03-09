package CommandRunner.gui.fxml;

import CommandRunner.*;
import CommandRunner.gui.CommandController;
import CommandRunner.gui.CommandStatus;
import CommandRunner.gui.CommandTableRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.converter.DefaultStringConverter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@SuppressWarnings("UnusedDeclaration")
public class GUIController implements Initializable, CommandQueueListener, CommandListener {

    @FXML
    private TableView<CommandTableRow> commandTable;

    @FXML
    private TableColumn<CommandTableRow, String> commandColumn;

    @FXML
    private TableColumn<CommandTableRow, String> statusColumn;

    @FXML
    private TableColumn<CommandTableRow, String> directoryColumn;

    @FXML
    private TableColumn<CommandTableRow, String> commentColumn;

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

    private List<CommandTableRow> dragRows;
    private int dragStartIndex;

    @FXML
    private Button addButton;

    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        assert commandTable != null : "fx:id=\"commandTable\" was not injected: check FXML file 'gui.fxml'.";

        commandTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        commandColumn.setCellValueFactory(cellData -> cellData.getValue().commandNameAndArgumentsProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().commandStatusProperty());
        commentColumn.setCellValueFactory(cellData -> cellData.getValue().commandCommentProperty());
        directoryColumn.setCellValueFactory(cellData -> cellData.getValue().commandDirectoryProperty());

        commandColumn.setOnEditCommit(t -> t.getTableView().getItems().get(
                t.getTablePosition().getRow()).setCommandNameAndArguments(t.getNewValue()));
        directoryColumn.setOnEditCommit(t -> t.getTableView().getItems().get(
                t.getTablePosition().getRow()).setCommandDirectory(t.getNewValue()));
        commentColumn.setOnEditCommit(t -> t.getTableView().getItems().get(
                t.getTablePosition().getRow()).setCommandComment(t.getNewValue()));

        commentColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        directoryColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        commandColumn.setCellFactory(param -> {
                    TableCell<CommandTableRow, String> cell = new TextFieldTableCell<CommandTableRow, String>(new DefaultStringConverter()) {
                        @Override
                        public void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) {
                                setText(null);
                            } else {
                                setText(item);
                            }
                        }
                    };

                    // highlight drop target by changing background color:
                    cell.setOnDragEntered(event -> cell.setStyle("-fx-background-color: gold;"));
                    cell.setOnDragExited(event -> cell.setStyle(""));
                    cell.setOnDragOver(event -> {
                        event.acceptTransferModes(TransferMode.MOVE);
                    });

                    cell.setOnDragDropped(event -> {
                        int dragEndIndex = cell.getIndex();
                        final List<CommandTableRow> commandTableItems = commandTable.getItems();
                        if (dragEndIndex == dragStartIndex || dragRows.size() == commandTableItems.size()) {
                            return;
                        }

                        moveRowsToIndex(dragEndIndex, dragRows);
                        dragRows = null;
                        event.consume();
                    });

                    cell.setOnDragDetected(event -> {
                        // drag was detected, start drag-and-drop gesture
                        ObservableList<CommandTableRow> selected = commandTable.getSelectionModel().getSelectedItems();
                        if (selected != null & !selected.isEmpty()) {
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

    private void moveRowsToIndex(int newIndex, List<CommandTableRow> dragRows) {
        List<CommandTableRow> commandTableItems = commandTable.getItems();
        commandTable.getSelectionModel().clearSelection();
        commandTableItems.removeAll(dragRows);

        int oldItemIndex = Math.max(0, Math.min(commandTableItems.size() - 1, newIndex));
        final CommandTableRow oldRow = commandTableItems.get(oldItemIndex);
        int index = commandTableItems.indexOf(oldRow);
        int indexToMoveTo = Math.max(0, newIndex);
        if (indexToMoveTo > commandTableItems.size()) {
            indexToMoveTo = commandTableItems.size();
        }
        commandTableItems.addAll(indexToMoveTo, dragRows);

        commandTable.getSelectionModel().selectRange(indexToMoveTo, indexToMoveTo + dragRows.size());
        dragRows.clear();
    }

    public void setItems(ObservableList<CommandTableRow> commandTableRows) {
        commandTable.setItems(commandTableRows);
    }

    public CommandTableRow removeCommandTableRow(int row) {
        return commandTable.getItems().remove(row);
    }

    public boolean addCommandTableRow(CommandTableRow row) {
        return commandTable.getItems().add(row);
    }

    @FXML
    private void removeCommandTableRow(Event event) {
        commandTable.getItems().removeAll(new ArrayList<>(commandTable.getSelectionModel().getSelectedItems()));
        commandTable.getSelectionModel().clearSelection();

        saveCommands();
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
        commandTable.getItems().add(
                new CommandTableRow(
                        new Command(
                                activeCommandController.getCommandDirectory(),
                                activeCommandController.getCommandNameAndArguments(),
                                ""
                        )
                )
        );
    }

    @FXML
    private void runSelected(ActionEvent event) {
        runCommandRows(commandTable.getSelectionModel().getSelectedItems());
    }

    @FXML
    private void runAll(ActionEvent event) {
        runCommandRows(commandTable.getItems());
    }

    private void runCommandRows(List<CommandTableRow> commandsToRun) {
        commandsToRun.forEach(row -> row.getCommand().setCommandStatus(CommandStatus.IDLE));
        commandQueue.setCommands(
                commandsToRun.stream()
                        .map(CommandTableRow::getCommand)
                        .collect(Collectors.toList())
        );
        commandTable.getSelectionModel().clearSelection();

        commandQueue.start();
    }

    @FXML
    private void tableKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case DELETE:
                removeCommandTableRow(event);
                break;
            case UP:
                keyboardMoveSelectedRows(event, -1);
                break;
            case DOWN:
                keyboardMoveSelectedRows(event, 1);
                break;
            default:
        }
    }

    private void keyboardMoveSelectedRows(KeyEvent event, int modifier) {
        if (event.isAltDown()) {
            final List<CommandTableRow> selected = new ArrayList<>(commandTable.getSelectionModel().getSelectedItems());
            final ObservableList<CommandTableRow> items = commandTable.getItems();
            if (selected.size() != items.size() && !selected.isEmpty()) {
                dragStartIndex = items.indexOf(selected.get(0));
                moveRowsToIndex(dragStartIndex + modifier, selected);
            }
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
    }

    @Override
    public void commandOutput(Command command, String text) {
        commandOutputArea.appendText(text + "\n");
    }

    @FXML
    private void save(ActionEvent event) {
        saveCommands();
    }

    private void saveCommands() {
        CommandRunner.getInstance().saveCommands(getCommands());
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

    private CommandTableRow findRowForCommand(Command command) {
        return commandTable.getItems().stream()
                .filter(row -> row.getCommand() == command)
                .collect(Collectors.toList())
                .get(0);
    }

    public void setCommands(List<Command> commands) {
        final ObservableList<CommandTableRow> commandTableRows = FXCollections.observableArrayList();
        commands.forEach(command -> commandTableRows.add(new CommandTableRow(command)));
        setItems(commandTableRows);
    }

    private List<Command> getCommands() {
        return commandTable.getItems().stream()
                .map(row -> row.getCommand())
                .collect(Collectors.toList());
    }
}
