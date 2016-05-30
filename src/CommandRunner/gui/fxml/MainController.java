package CommandRunner.gui.fxml;

import CommandRunner.CommandRunner;
import CommandRunner.gui.LayoutChangedListener;
import CommandRunner.gui.commandqueuetree.LimitTextArea;
import CommandRunner.gui.StatusBarController;
import CommandRunner.gui.WindowLayout;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static CommandRunner.gui.WindowLayout.DEFAULT_LAYOUT;

@SuppressWarnings("UnusedDeclaration")
public class MainController implements Initializable {
    @FXML
    private SplitPane verticalSplitPane;
    @FXML
    private SplitPane horizontalSplitPane;
    @FXML
    private TreeView<CommandQueueTreeRow> commandQueueTreeView;
    @FXML
    private TreeTableView<CommandTableRow> commandTable;
    @FXML
    private TreeTableColumn<CommandTableRow, String> commandColumn;
    @FXML
    private TreeTableColumn<CommandTableRow, String> directoryColumn;
    @FXML
    private TreeTableColumn<CommandTableRow, String> commentColumn;
    @FXML
    private LimitTextArea commandOutputArea;
    @FXML
    private HBox statusBar;
    @FXML
    private Rectangle memoryBar;
    @FXML
    private Label memoryLabel;
    @FXML
    private CheckMenuItem lightThemeMenuItem;
    @FXML
    private CheckMenuItem darkThemeMenuItem;
    @FXML
    private CheckMenuItem statusBarMenuItem;
    @FXML
    private VBox mainContainer;
    @FXML
    private TextField inputTextField;
    @FXML
    private Button addButton;

    private CommandQueueTreeController commandQueueTreeController;
    private CommandTableController commandTableController;
    private List<LayoutChangedListener> layoutChangeListeners;

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        System.out.println("javafx.runtime.version: " + System.getProperties().get("javafx.runtime.version"));
        assert commandTable != null : "fx:id=\"commandTable\" was not injected: check FXML file 'main .fxml'.";
        commandTableController = new CommandTableController(commandTable, commandColumn, directoryColumn, commentColumn);
        commandQueueTreeController = new CommandQueueTreeController(commandQueueTreeView, commandOutputArea);
        commandTable.addEventFilter(KeyEvent.KEY_PRESSED, this::tableKeyPressed);
        StatusBarController statusBarController = new StatusBarController();
        statusBarController.doStuff(memoryBar, memoryLabel);

        layoutChangeListeners = new ArrayList<>();
        CommandRunner.getInstance().controllerLoaded(this);

        inputTextField.setOnAction(event -> sendInput());
    }

    @FXML
    private void addSelectedItemsToGroup(Event event) {
        commandTableController.addSelectedItemsToGroup();
    }

    @FXML
    private void removeCommandTableRow(Event event) {
        commandTableController.removeSelectedCommandTableRows();
    }

    @FXML
    private void addCommandTableRow(Event event) {
        commandTableController.addCommandTableRow();
    }

    @FXML
    private void runSelected(Event event) {
        commandQueueTreeController.stopAppendingText();
        commandTableController.runSelected(commandQueueTreeController, CommandRunner.getInstance());
    }

    @FXML
    private void save(Event event) {
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

    @FXML
    private void runSelectedInParallel(Event event) {
        commandQueueTreeController.stopAppendingText();
        commandTableController.runSelectedInParallel(commandQueueTreeController);
    }

    public void resetLayout(ActionEvent event) {
        Stage primaryStage = CommandRunner.getInstance().getPrimaryStage();

        primaryStage.setWidth(DEFAULT_LAYOUT.getWindowWidth());
        primaryStage.setHeight(DEFAULT_LAYOUT.getWindowHeight());
        primaryStage.setMaximized(false);
        primaryStage.centerOnScreen();

        verticalSplitPane.setDividerPosition(0, DEFAULT_LAYOUT.getVerticalDividerPosition());
        horizontalSplitPane.setDividerPosition(0, DEFAULT_LAYOUT.getHorizontalDividerPosition());
        commandColumn.setPrefWidth(DEFAULT_LAYOUT.getTableCommandColumnWidth());
        directoryColumn.setPrefWidth(DEFAULT_LAYOUT.getTableDirectoryColumnWidth());
        commentColumn.setPrefWidth(DEFAULT_LAYOUT.getTableCommentColumnWidth());

        verticalSplitPaneChanged(DEFAULT_LAYOUT.getVerticalDividerPosition());
        horizontalSplitPaneChanged(DEFAULT_LAYOUT.getHorizontalDividerPosition());
        commandColumnWidthChanged(DEFAULT_LAYOUT.getTableCommandColumnWidth());
        directoryColumnWidthChanged(DEFAULT_LAYOUT.getTableDirectoryColumnWidth());
        commentColumnWidthChanged(DEFAULT_LAYOUT.getTableCommentColumnWidth());
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
                    if (event.isControlDown()) {
                        runSelectedInParallel(event);
                    } else {
                        runSelected(event);
                    }
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
            case S:
                if (event.isControlDown()) {
                    save(event);
                }
                break;
            case C:
                if (event.isControlDown() && commandTable.getEditingCell() == null) {
                    commandTableController.copySelectedToClipBoard();
                }
                consume = false;
                break;
            case X:
                if (event.isControlDown() && commandTable.getEditingCell() == null) {
                    commandTableController.cutSelectedToClipBoard();
                }
                consume = false;
                break;
            case V:
                if (event.isControlDown() && commandTable.getEditingCell() == null) {
                    commandTableController.pasteSelectedFromClipBoard();
                }
                consume = false;
                break;
            case N:
                if (event.isControlDown()) {
                    addCommandTableRow(event);
                }
                break;
            case G:
                if (event.isControlDown()) {
                    addSelectedItemsToGroup(event);
                }
                break;
            case F2:
                if (event.isShiftDown()) {
                    consume = commandTableController.editSelected(directoryColumn);
                } else if (event.isControlDown()) {
                    consume = commandTableController.editSelected(commentColumn);
                } else {
                    consume = commandTableController.editSelected(commandColumn);
                }
                break;
            default:
                consume = false;
        }

        if (consume) {
            event.consume();
        }
    }

    public void addLayoutChangedListener(LayoutChangedListener listener) {
        layoutChangeListeners.add(listener);
    }

    public void setLayout(WindowLayout windowLayout) {
        verticalSplitPane.setDividerPosition(0, windowLayout.getVerticalDividerPosition());
        horizontalSplitPane.setDividerPosition(0, windowLayout.getHorizontalDividerPosition());
        commandColumn.setPrefWidth(windowLayout.getTableCommandColumnWidth());
        directoryColumn.setPrefWidth(windowLayout.getTableDirectoryColumnWidth());
        commentColumn.setPrefWidth(windowLayout.getTableCommentColumnWidth());
        checkThemeLayoutItems(windowLayout.getTheme().equals(WindowLayout.LIGHT_THEME));
        showStatusBar(windowLayout.isShowStatusBar());

        commandTable.requestFocus();
    }

    private void verticalSplitPaneChanged(Number newValue) {
        layoutChangeListeners.forEach(l -> l.verticalDividerPositionChanged(newValue.doubleValue()));
    }

    private void horizontalSplitPaneChanged(Number newValue) {
        layoutChangeListeners.forEach(l -> l.horizontalDividerPositionChanged(newValue.doubleValue()));
    }

    private void commandColumnWidthChanged(Number newValue) {
        layoutChangeListeners.forEach(l -> l.tableCommandColumnWidthChanged(newValue.intValue()));
    }

    private void commentColumnWidthChanged(Number newValue) {
        layoutChangeListeners.forEach(l -> l.tableCommentColumnWidthChanged(newValue.intValue()));
    }

    private void directoryColumnWidthChanged(Number newValue) {
        layoutChangeListeners.forEach(l -> l.tableDirectoryColumnWidthChanged(newValue.intValue()));
    }

    public void addChangeListeners() {
        verticalSplitPane.getDividers().get(0).positionProperty().addListener((observable, oldValue, newValue) -> verticalSplitPaneChanged(newValue));
        horizontalSplitPane.getDividers().get(0).positionProperty().addListener((observable, oldValue, newValue) -> horizontalSplitPaneChanged(newValue));
        commandColumn.widthProperty().addListener((observable, oldValue, newValue) -> commandColumnWidthChanged(newValue));
        commentColumn.widthProperty().addListener((observable, oldValue, newValue) -> commentColumnWidthChanged(newValue));
        directoryColumn.widthProperty().addListener((observable, oldValue, newValue) -> directoryColumnWidthChanged(newValue));
    }

    public void lightThemeSelected(ActionEvent actionEvent) {
        layoutChangeListeners.forEach(l -> l.themeChanged(WindowLayout.LIGHT_THEME));
        CommandRunner.getInstance().themeChanged();
        checkThemeLayoutItems(true);
    }

    public void darkThemeSelected(ActionEvent actionEvent) {
        layoutChangeListeners.forEach(l -> l.themeChanged(WindowLayout.DARK_THEME));
        CommandRunner.getInstance().themeChanged();
        checkThemeLayoutItems(false);
    }

    private void checkThemeLayoutItems(boolean isLightSelected) {
        lightThemeMenuItem.setSelected(isLightSelected);
        darkThemeMenuItem.setSelected(!isLightSelected);
    }

    public void toggleStatusBar(ActionEvent actionEvent) {
        boolean showStatusBar = statusBarMenuItem.isSelected();
        layoutChangeListeners.forEach(l -> l.showStatusBarChanged(showStatusBar));
        showStatusBar(showStatusBar);
    }

    private void showStatusBar(boolean showStatusBar) {
        statusBarMenuItem.setSelected(showStatusBar);

        mainContainer.getChildren().remove(statusBar);
        if (showStatusBar) {
            mainContainer.getChildren().add(statusBar);
        }
    }

    private void sendInput() {
        commandQueueTreeController.sendInput(inputTextField.getText());
        inputTextField.clear();
    }
}
