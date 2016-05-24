package CommandRunner;

import CommandRunner.gui.WindowLayout;
import CommandRunner.gui.commandtable.CommandTableCommandRow;
import CommandRunner.gui.commandtable.CommandTableRow;
import CommandRunner.gui.fxml.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static CommandRunner.gui.commandtable.CommandTableRowTreeItemListManipulator.addAllCommandRowsForTreeItem;
import static CommandRunner.gui.commandtable.CommandTableRowTreeItemListManipulator.getFlatTreeItemList;

public class CommandRunner extends Application implements CommandQueueListener, CommandListener {
    public static final String PROGRAM_TITLE = "Command Runner";
    public static final String PROGRAM_VERSION = "0.2";

    private static final String SETTINGS_FXML = "gui/fxml/settings.fxml";
    private static final String ABOUT_FXML = "gui/fxml/about.fxml";

    private static CommandRunner instance = null;

    private final ProgramState programState;

    private String runCommand = null;
    private Stage primaryStage;
    private TreeItem<CommandTableRow> rootNode;

    @SuppressWarnings("unused")
    public CommandRunner() throws Exception {
        if (instance != null) {
            throw new Exception("There can be only one");
        }
        programState = new ProgramState();
        instance = this;
    }

    private FXMLLoader getFXML(String fxml) {
        return new FXMLLoader(getClass().getResource(fxml));
    }

    public static CommandRunner getInstance() {
        return instance;
    }

    @Override
    public void init() throws Exception {
        super.init();

        final Parameters parameters = getParameters();
        final Map<String, String> namedParameters = parameters.getNamed();
        runCommand = namedParameters.get("run");
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        rootNode = loadSettings();

        if (runCommand != null) {
            runCommand(runCommand);
        } else {
            Parent root = FXMLLoader.load(getClass().getResource("gui/fxml/main.fxml"));
            primaryStage.setTitle(PROGRAM_TITLE);
            primaryStage.getIcons().add(new Image("png/icon.png"));
            primaryStage.setScene(createScene(root));
            primaryStage.setOnCloseRequest(this::onClose);
            WindowLayout windowLayout = programState.getWindowLayout();
            primaryStage.setWidth(windowLayout.getWindowWidth());
            primaryStage.setHeight(windowLayout.getWindowHeight());
            primaryStage.setMaximized(windowLayout.isMaximized());
            primaryStage.show();
        }
    }

    private void onClose(WindowEvent event) {
        programState.saveLayout(primaryStage.getWidth(), primaryStage.getHeight(), primaryStage.isMaximized());
        switch (programState.getSaveOnExit()) {
            case ASK:
                if (programState.hasChangesSinceLastSave()) {
                    showAskSaveAlert(event);
                }
                break;
            case SAVE:
                save();
            case FORGET:
            default:
        }
    }

    private void showAskSaveAlert(WindowEvent event) {
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Save changes?");
        alert.setHeaderText("You are about to exit the program with unsaved changes.");
        alert.setContentText("If you do not save, all changes since the last save will be lost.");

        final ButtonType buttonTypeSave = new ButtonType("Save", ButtonBar.ButtonData.YES);
        final ButtonType buttonTypeExit = new ButtonType("Exit without saving", ButtonBar.ButtonData.NO);
        final ButtonType buttonTypeCancelClose = new ButtonType("Cancel exit", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeSave, buttonTypeCancelClose, buttonTypeExit);

        alert.showAndWait().ifPresent(result -> {
            if (result == buttonTypeSave) {
                save();
            } else if (result == buttonTypeCancelClose) {
                event.consume();
            }
        });
    }

    private Scene createScene(Parent root) {
        Scene scene = new Scene(root);
        scene.getStylesheets().add("css/style.css");
        return scene;
    }

    public void addSettingsStage() throws IOException {
        FXMLLoader loader = getFXML(SETTINGS_FXML);
        Parent root = loader.load();
        Stage settingsStage = new Stage();
        settingsStage.setTitle("ProgramState");
        settingsStage.getIcons().add(new Image("png/icon.png"));
        settingsStage.setScene(createScene(root));
        settingsStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void controllerLoaded(MainController controller) {
        controller.setRoot(rootNode);
        controller.addLayoutChangedListener(programState);

        Platform.runLater(() -> {
            controller.setLayout(programState.getWindowLayout());
            controller.addChangeListeners();
        });
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void save(TreeItem<CommandTableRow> node) {
        programState.saveCommands(node);
    }

    private void save() {
        programState.saveSettings();
        programState.saveCommands(rootNode);
    }

    private TreeItem<CommandTableRow> loadSettings() {
        programState.load();
        return programState.getRoot();
    }

    public ProgramState getProgramState() {
        return programState;
    }

    private void runCommand(String commandComment) {
        runAllCommandsWithComment(commandComment, loadSettings());
    }

    private void runAllCommandsWithComment(String comment, TreeItem<CommandTableRow> root) {
        if (comment == null || comment.equals("")) {
            return;
        }

        final List<TreeItem<CommandTableRow>> commandsWithComment = getFlatTreeItemList(root).stream()
                .filter(commandItem -> commandItem.getValue().commandCommentProperty().getValue().equals(comment))
                .collect(Collectors.toList());

        runCommandTreeItems(commandsWithComment, this);
    }

    public void runCommandTreeItems(List<TreeItem<CommandTableRow>> treeItemsToRun, CommandQueueListener... listeners) {
        List<CommandTableCommandRow> commandTableRowsToRun = new ArrayList<>();
        treeItemsToRun.forEach(item -> addAllCommandRowsForTreeItem(item, commandTableRowsToRun));
        CommandQueue commandQueue = new CommandQueue(listeners);

        commandQueue.setCommands(
                commandTableRowsToRun.stream()
                        .map(CommandTableCommandRow::getCommand)
                        .map(Command::copy)
                        .collect(Collectors.toList())
        );

        commandQueue.start();
    }


    @Override
    public void commandQueueStarted(CommandQueue commandQueue) {

    }

    @Override
    public void commandQueueFinished(CommandQueue commandQueue) {
        System.exit(0);
    }

    @Override
    public void commandQueueIsProcessing(Command command) {
        System.out.println("--- executing " + command.getCommandNameAndArguments() + " ----");
        command.addCommandListener(this);
    }

    @Override
    public void commandExecuted(Command command) {
    }

    @Override
    public void commandOutput(Command command, String text) {
        System.out.println(text);
    }

    public void addAboutStage() throws IOException {
        FXMLLoader loader = getFXML(ABOUT_FXML);
        Parent root = loader.load();
        Stage settingsStage = new Stage();
        settingsStage.setTitle("About");
        settingsStage.getIcons().add(new Image("png/icon.png"));
        settingsStage.setScene(createScene(root));
        settingsStage.show();
    }
}
