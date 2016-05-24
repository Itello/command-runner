package CommandRunner.gui.fxml;

import CommandRunner.CommandRunner;
import CommandRunner.gui.commandqueuetree.CommandQueueTreeController;
import CommandRunner.gui.commandqueuetree.CommandQueueTreeRow;
import CommandRunner.gui.commandtable.CommandTableController;
import CommandRunner.gui.commandtable.CommandTableRow;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@SuppressWarnings("UnusedDeclaration")
public class MainController implements Initializable {

    @FXML
    public TreeView<CommandQueueTreeRow> commandQueueTreeView;

    @FXML
    private TreeTableView<CommandTableRow> commandTable;

    @FXML
    private TreeTableColumn<CommandTableRow, String> commandColumn;

    @FXML
    private TreeTableColumn<CommandTableRow, String> directoryColumn;

    @FXML
    private TreeTableColumn<CommandTableRow, String> commentColumn;

    @FXML
    private TextArea commandOutputArea;

    private CommandQueueTreeController commandQueueTreeController;

    private CommandTableController commandTableController;

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        assert commandTable != null : "fx:id=\"commandTable\" was not injected: check FXML file 'main .fxml'.";

        commandTableController = new CommandTableController(commandQueueTreeView, commandTable, commandColumn, directoryColumn, commentColumn);

        commandQueueTreeController = new CommandQueueTreeController(commandQueueTreeView, commandOutputArea);

        commandTable.addEventFilter(KeyEvent.KEY_PRESSED, this::tableKeyPressed);

        CommandRunner.getInstance().controllerLoaded(this);
    }

    @FXML
    private void addSelectedItemsToGroup(Event event) {
        commandTableController.addSelectedItemsToGroup("Group");
    }

    @FXML
    private void removeCommandTableRow(Event event) {
        commandTableController.removeCommandTableRow(event);
    }

    @FXML
    private void addCommandTableRow(ActionEvent event) {
        commandTableController.addCommandTableRow(event);
    }

    @FXML
    private void runSelected(Event event) {
        commandTableController.runSelected(commandQueueTreeController);
    }

    @FXML
    private void save(ActionEvent event) {
        save();
    }

    private void save() {
        CommandRunner.getInstance().save(getRoot());
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

    private TreeItem<CommandTableRow> getRoot() {
        return commandTableController.getRoot();
    }

    public void setRoot(TreeItem<CommandTableRow> commandTreeNode) {
        commandTableController.setRoot(commandTreeNode);
    }

    public void close(ActionEvent event) {
        final Stage stage = CommandRunner.getInstance().getPrimaryStage();
        stage.fireEvent(
                new WindowEvent(
                        stage,
                        WindowEvent.WINDOW_CLOSE_REQUEST
                )
        );
    }

    public void deleteSelectedFromTable(ActionEvent event) {
        removeCommandTableRow(event);
    }

    public void resetLayout(ActionEvent event) {
        // TODO
    }

    public void about(ActionEvent event) throws IOException {
        CommandRunner.getInstance().addAboutStage();
    }

    public void clearQueue(ActionEvent event) {
        commandQueueTreeController.clearQueue();
    }

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
                commandTableController.keyboardMoveSelectedRows(-1);
                break;
            case PAGE_DOWN:
                commandTableController.keyboardMoveSelectedRows(1);
                break;
            default:
                consume = false;
        }

        if (consume) {
            event.consume();
        }
    }
}
