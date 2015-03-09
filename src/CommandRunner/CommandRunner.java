package CommandRunner;

import CommandRunner.gui.fxml.GUIController;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class CommandRunner extends Application {

    private static final String ADD_COMMAND_FXML = "gui/fxml/addCommand.fxml";
    private static final String SETTINGS_FXML = "gui/fxml/settings.fxml";
    private static final String PROGRAM_TITLE = "Command Runner";

    private static CommandRunner instance = null;

    private Stage primaryStage;

    private final Settings settings;

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
        controller.setCommands(loadCommands());
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void saveCommands(List<Command> commands) {
        settings.setCommands(commands);
        settings.setHaltOnError(false);
        settings.save();
    }

    public List<Command> loadCommands() {
        settings.load();
        return settings.getCommands();
    }

    public Settings getSettings() {
        return settings;
    }
}
