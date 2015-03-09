package CommandRunner;

import CommandRunner.gui.fxml.GUIController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//TODO: CSS Styling
//TODO: Halt on error setting, (enable more than commands in savefile)
//TODO: Make kill actually kill

public class CommandRunner extends Application {

    private static final String ADD_COMMAND_FXML = "gui/fxml/addCommand.fxml";
    private static final String DIRECTORY_STRING = "directory";
    private static final String COMMAND_AND_ARGUMENTS_STRING = "commandsAndArguments";
    private static final String COMMAND_COMMENT_STRING = "commandComment";
    private static final File SAVE_FILE = new File(System.getProperty("user.home") + System.getProperty("file.separator") + ".commandRunner");
    private static final String PROGRAM_TITLE = "Command Runner";

    private static CommandRunner instance = null;

    private Stage primaryStage;

    public CommandRunner() throws Exception {
        if (instance != null) {
            throw new Exception("There can be only one");
        }
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
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
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
        String content = String.join("\n", commands.stream()
                .map(this::convertCommandToKeyValueString)
                .collect(Collectors.toList()));
        saveFile(content, SAVE_FILE);
    }

    private String convertCommandToKeyValueString(Command command) {
        return DIRECTORY_STRING + "=" + command.getCommandDirectory() + ";"
                + COMMAND_AND_ARGUMENTS_STRING + "=" + command.getCommandNameAndArguments() + ";"
                + COMMAND_COMMENT_STRING + "=" + command.getCommandComment();
    }

    public List<Command> loadCommands() {
        final List<Command> commands = new ArrayList<>();
        if (SAVE_FILE.exists()) {
            try {
                Files.lines(SAVE_FILE.toPath()).forEach(
                        line -> {
                            Map<String, String> map = new HashMap<>();
                            for (String keyValuePair : line.split(";")) {
                                String[] keyValue = keyValuePair.split("=");
                                String key = keyValue[0];
                                String value = "";
                                if (keyValue.length > 1) {
                                    value = keyValue[1];
                                }
                                map.put(key, value);
                            }
                            commands.add(new Command(map.get(DIRECTORY_STRING), map.get(COMMAND_AND_ARGUMENTS_STRING), map.get(COMMAND_COMMENT_STRING)));
                        }
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return commands;
    }

    private void saveFile(String content, File file) {
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(content);
            fileWriter.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
