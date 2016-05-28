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
import java.util.*;
import java.util.stream.Collectors;

import static CommandRunner.CommandStatus.sortCommandStatuses;
import static CommandRunner.gui.commandtable.CommandTableRowTreeItemListManipulator.addAllCommandRowsForTreeItem;
import static CommandRunner.gui.commandtable.CommandTableRowTreeItemListManipulator.getFlatTreeItemList;

public class CommandRunner extends Application implements CommandQueueListener, CommandListener {
    public static final String PROGRAM_TITLE = "Command Runner";
    public static final String PROGRAM_VERSION = "0.2b";

    private static final String SETTINGS_FXML = "gui/fxml/settings.fxml";
    private static final String ABOUT_FXML = "gui/fxml/about.fxml";
    private static final String VARIABLE_SYMBOL = "#";

    private static CommandRunner instance = null;

    private final ProgramState programState;
    private String runCommand = null;
    private String variables = null;
    private Stage primaryStage;
    private TreeItem<CommandTableRow> rootNode;
    private boolean runningWithParameters = false;
    private ArrayDeque<CommandQueue> runningQueues;

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
        variables = namedParameters.get("variables");
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        rootNode = loadSettings();

        if (runCommand != null) {
            runningWithParameters = true;
            runCommand(runCommand, variables);
        } else {
            Parent root = FXMLLoader.load(getClass().getResource("gui/fxml/main.fxml"));
            primaryStage.setTitle(PROGRAM_TITLE);
            primaryStage.getIcons().add(new Image("png/command.png"));
            primaryStage.setScene(createScene(root));
            primaryStage.setOnCloseRequest(this::onClose);
            WindowLayout windowLayout = programState.getWindowLayout();
            primaryStage.setWidth(windowLayout.getWindowWidth());
            primaryStage.setHeight(windowLayout.getWindowHeight());
            primaryStage.setMaximized(windowLayout.isMaximized());
            primaryStage.show();
            runningQueues = new ArrayDeque<>();
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
        settingsStage.setTitle("Settings");
        settingsStage.getIcons().add(new Image("png/command.png"));
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

    private void runCommand(String commandComment, String variables) {
        runAllCommandsWithCommentAndVariables(commandComment, variables, loadSettings());
    }

    private void runAllCommandsWithCommentAndVariables(String comment, String variables, TreeItem<CommandTableRow> root) {
        if (comment == null || comment.equals("")) {
            return;
        }

        final List<TreeItem<CommandTableRow>> commandsWithComment = getFlatTreeItemList(root).stream()
                .filter(commandItem -> commandItem.getValue().commandCommentProperty().getValue().equals(comment))
                .collect(Collectors.toList());

        Deque<TreeItem<CommandTableRow>> commandRowsToReplace = new ArrayDeque<>();
        commandRowsToReplace.addAll(commandsWithComment);
        while (!commandRowsToReplace.isEmpty()) {
            TreeItem<CommandTableRow> node = commandRowsToReplace.pop();
            commandRowsToReplace.addAll(node.getChildren());
            replaceCommandAndDirectoryWithVariables(variables, node);
        }

        runCommandTreeItems(commandsWithComment, this);
    }

    private void replaceCommandAndDirectoryWithVariables(String variables, TreeItem<CommandTableRow> commandWithComment) {
        Map<String, String> variableMap = getVariableMap(variables);
        if (!variableMap.isEmpty()) {
            CommandTableRow value = commandWithComment.getValue();
            value.setCommandNameAndArguments(replaceVariables(value.commandNameAndArgumentsProperty().getValue(), variableMap));
            value.setCommandDirectory(replaceVariables(value.commandDirectoryProperty().getValue(), variableMap));
        }
    }

    private String replaceVariables(String value, Map<String, String> variableMap) {
        String returnValue = value;
        for (Map.Entry<String, String> entry : variableMap.entrySet()) {
            returnValue = returnValue.replaceAll(VARIABLE_SYMBOL + entry.getKey(), entry.getValue());
        }

        return returnValue;
    }

    private Map<String, String> getVariableMap(String variables) {
        if (variables == null || variables.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> variableMap = new HashMap<>();
        Arrays.stream(variables.split(","))
                .map(entry -> entry.split("="))
                .forEach(kvPair -> variableMap.put(kvPair[0], kvPair[1]));

        return variableMap;
    }

    public void runCommandTreeItems(List<TreeItem<CommandTableRow>> treeItemsToRun, CommandQueueListener... listeners) {
        List<CommandTableCommandRow> commandTableRowsToRun = getCommandTableCommandRowsToRun(treeItemsToRun);
        CommandQueue commandQueue = new CommandQueue(listeners);

        commandQueue.setCommands(
                commandTableRowsToRun.stream()
                        .map(CommandTableCommandRow::getCommand)
                        .map(Command::copy)
                        .collect(Collectors.toList())
        );

        commandQueue.start();
    }

    private List<CommandTableCommandRow> getCommandTableCommandRowsToRun(List<TreeItem<CommandTableRow>> treeItemsToRun) {
        List<CommandTableCommandRow> commandTableRowsToRun = new ArrayList<>();
        treeItemsToRun.forEach(item -> addAllCommandRowsForTreeItem(item, commandTableRowsToRun));
        return commandTableRowsToRun;
    }

    public void runCommandTreeItemsInParallel(List<TreeItem<CommandTableRow>> treeItemsToRun, CommandQueueListener... listeners) {
        List<CommandTableCommandRow> commandTableRowsToRun = getCommandTableCommandRowsToRun(treeItemsToRun);

        commandTableRowsToRun.forEach(row -> {
            final CommandQueue commandQueue = new CommandQueue(listeners);
            commandQueue.setCommands(Collections.singletonList(row.getCommand().copy()));
            commandQueue.start();
        });
    }

    @Override
    public void commandQueueStarted(CommandQueue commandQueue) {
        if (!runningWithParameters) {
            runningQueues.addFirst(commandQueue);
            setTitleWithCommandQueueStatus();
        }
    }

    @Override
    public void commandQueueFinished(CommandQueue commandQueue) {
        if (!runningWithParameters) {
            setTitleWithCommandQueueStatus();
            runningQueues.remove(commandQueue);
        } else {
            System.exit(0);
        }
    }

    @Override
    public void commandQueueIsProcessing(Command command) {
        if (!runningWithParameters) {
            setTitleWithCommandQueueStatus();
        } else {
            System.out.println("--- executing " + command.getCommandNameAndArguments() + " ----");
            command.addCommandListener(this);
        }
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
        settingsStage.getIcons().add(new Image("png/command.png"));
        settingsStage.setScene(createScene(root));
        settingsStage.show();
    }

    private void setTitleWithCommandQueueStatus() {
        if (runningQueues.isEmpty()) {
            return;
        }

        List<CommandStatus> commandStatusList = runningQueues.stream()
                .map(CommandQueue::getCommandStatus)
                .collect(Collectors.toList());
        sortCommandStatuses(commandStatusList);

        primaryStage.setTitle(commandStatusList.get(0).toString().toLowerCase() + " - " + PROGRAM_TITLE);
    }
}
