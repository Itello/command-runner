package CommandRunner;

import CommandRunner.gui.CommandTableCommandRow;
import CommandRunner.gui.CommandTableRow;
import javafx.scene.control.TreeItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;

public class Settings {

    public enum SaveOnExit {
        ASK,
        SAVE,
        FORGET
    }

    private static final File SAVE_FILE = new File(System.getProperty("user.home") + System.getProperty("file.separator") + ".commandRunner");
    private static final String COMMANDS = "commands";
    private static final String HALT_ON_ERROR = "haltOnError";
    private static final String CONFIRM_NONEMPTY_DELETE = "confirmNonemptyDelete";
    private static final String SAVE_ON_EXIT = "saveOnExit";

    private TreeItem<CommandTableRow> root = null;
    private boolean haltOnError = true;
    private boolean confirmNonemptyDelete = true;
    private SaveOnExit saveOnExit = SaveOnExit.ASK;

    Settings() {

    }

    public void saveSettingsButKeepCommands() {
        save(root);
    }

    public void save(TreeItem<CommandTableRow> root) {
        this.root = root;

        try {
            JSONObject commandsObject = new JSONObject();
            appendNodeHierarchyToJSON(root, commandsObject);

            JSONObject settingsObject = new JSONObject();
            settingsObject.put(COMMANDS, commandsObject);
            settingsObject.put(HALT_ON_ERROR, haltOnError);
            settingsObject.put(CONFIRM_NONEMPTY_DELETE, confirmNonemptyDelete);
            settingsObject.put(SAVE_ON_EXIT, saveOnExit.toString());

            FileWriter fileWriter = new FileWriter(SAVE_FILE);
            fileWriter.write(settingsObject.toString(1));
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void appendNodeHierarchyToJSON(TreeItem<CommandTableRow> node, JSONObject object) throws JSONException {
        final CommandTableRow commandTableRow = node.getValue();

        if (commandTableRow instanceof CommandTableCommandRow) {
            Command command = ((CommandTableCommandRow) commandTableRow).getCommand();
            JSONObject commandObject = new JSONObject();
            commandObject.put(JSONFileReader.DIRECTORY_STRING, command.getCommandDirectory());
            commandObject.put(JSONFileReader.COMMAND_AND_ARGUMENTS_STRING, command.getCommandNameAndArguments());
            commandObject.put(JSONFileReader.COMMAND_COMMENT_STRING, command.getCommandComment());
            object.put(JSONFileReader.COMMAND, commandObject);
        } else {
            object.put(JSONFileReader.NAME, commandTableRow.commandNameAndArgumentsProperty().getValue());
            JSONArray array = new JSONArray();
            for (TreeItem<CommandTableRow> child : node.getChildren()) {
                JSONObject childJSON = new JSONObject();
                appendNodeHierarchyToJSON(child, childJSON);
                array.put(childJSON);
            }
            object.put(JSONFileReader.CHILDREN, array);
        }
    }

    private void load(boolean onlyCommands) {
        if (SAVE_FILE.exists()) {
            try {
                final JSONObject settingsObject = JSONFileReader.readJsonObjectFromFile(SAVE_FILE);

                setRoot(JSONFileReader.createNode(settingsObject.getJSONObject(COMMANDS)));
                if (!onlyCommands) {
                    haltOnError = settingsObject.getBoolean(HALT_ON_ERROR);
                    confirmNonemptyDelete = settingsObject.getBoolean(CONFIRM_NONEMPTY_DELETE);
                    saveOnExit = SaveOnExit.valueOf(settingsObject.getString(SAVE_ON_EXIT));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    void load() {
        load(false);
    }

    public void setHaltOnError(boolean halt) {
        haltOnError = halt;
    }

    public void setConfirmNonemptyDelete(boolean confirm) {
        confirmNonemptyDelete = confirm;
    }

    public boolean getHaltOnError() {
        return haltOnError;
    }

    public boolean getConfirmNonemptyDelete() {
        return confirmNonemptyDelete;
    }

    public SaveOnExit getSaveOnExit() {
        return saveOnExit;
    }

    public void setSaveOnExit(SaveOnExit saveOnExit) {
        this.saveOnExit = saveOnExit;
    }

    private void setRoot(TreeItem<CommandTableRow> root) {
        this.root = root;
    }

    public TreeItem<CommandTableRow> getRoot() {
        return root;
    }
}
