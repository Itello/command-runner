package CommandRunner;

import CommandRunner.gui.fxml.GUIController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class CommandRunner extends Application {

    private static final String ADD_COMMAND_FXML = "gui/fxml/addCommand.fxml";
    private static final String SETTINGS_FXML = "gui/fxml/settings.fxml";
    private static final String PROGRAM_TITLE = "Command Runner";

    private static CommandRunner instance = null;

    private Stage primaryStage;

    private final Settings settings;
    private GUIController guiController;

    public CommandRunner() throws Exception {
        if (instance != null) {
            throw new Exception("There can be only one");
        }
        settings = new Settings();
        instance = this;
    }

    public FXMLLoader getCommandFXML() {
        return getFXML(ADD_COMMAND_FXML);
    }

    private FXMLLoader getFXML(String resource) {
        return new FXMLLoader(getClass().getResource(resource));
    }

    public static CommandRunner getInstance() {
        return instance;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;

        Parent root = FXMLLoader.load(getClass().getResource("gui/fxml/gui.fxml"));
        primaryStage.setTitle(PROGRAM_TITLE);
        primaryStage.getIcons().add(new Image("png/icon.png"));
        primaryStage.setScene(createScene(root));
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            onClose();
        });
    }

    private void onClose() {
        switch (settings.getSaveOnExit()) {
            case ASK:
                if (guiController.hasChangesSinceLastSave()) {
                    showAskSaveAlert();
                }
                break;
            case SAVE:
                save();
            case FORGET:
            default:
        }
    }

    private void showAskSaveAlert() {
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Save changes?");
        alert.setHeaderText("You are about to exit the program with unsaved changes.");
        alert.setContentText("If you do not save, all changes since the last save will be lost.");

        final ButtonType buttonTypeOK = new ButtonType("Save");
        final ButtonType buttonTypeCancel = new ButtonType("Exit without saving", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeOK, buttonTypeCancel);

        final Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == buttonTypeOK) {
            save();
        }
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
        settingsStage.getIcons().add(new Image("png/icon.png"));
        settingsStage.setScene(createScene(root));
        settingsStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void controllerLoaded(GUIController controller) {
        final CommandTreeNode root = loadSettings();
        controller.setRoot(root);
        guiController = controller;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void save(CommandTreeNode node) {
        settings.save(node);
    }

    public void save() {
        guiController.save();
    }

    public CommandTreeNode loadSettings() {
        settings.load();
        return settings.getRoot();
    }

    public Settings getSettings() {
        return settings;
    }
}
