package CommandRunner;

import CommandRunner.gui.commandtable.CommandTableRow;
import javafx.scene.control.TreeItem;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;

import static CommandRunner.JsonConverter.appendNodeHierarchyToJSON;

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

    public void saveSettingsButKeepCommands() {
        save(root);
    }

    void save(TreeItem<CommandTableRow> root) {
        this.root = root;

        try {
            JSONObject commandsObject = getCommandsObject(root);

            JSONObject settingsObject = new JSONObject();
            settingsObject.put(COMMANDS, commandsObject);
            settingsObject.put(HALT_ON_ERROR, haltOnError);
            settingsObject.put(CONFIRM_NONEMPTY_DELETE, confirmNonemptyDelete);
            settingsObject.put(SAVE_ON_EXIT, saveOnExit.toString());

            FileWriter fileWriter = new FileWriter(SAVE_FILE);
            fileWriter.write(settingsObject.toString(2));
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void load() {
        if (SAVE_FILE.exists()) {
            try {
                final String settingsString = JSONFileReader.readJsonObjectFromFile(SAVE_FILE);
                JSONObject settingsObject = JsonConverter.convertFromJSONToObject(settingsString);

                setRoot(JSONFileReader.createNode(settingsObject.getJSONObject(COMMANDS)));
                haltOnError = settingsObject.getBoolean(HALT_ON_ERROR);
                confirmNonemptyDelete = settingsObject.getBoolean(CONFIRM_NONEMPTY_DELETE);
                saveOnExit = SaveOnExit.valueOf(settingsObject.getString(SAVE_ON_EXIT));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
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

    TreeItem<CommandTableRow> getRoot() {
        return root;
    }

    boolean hasChangesSinceLastSave() {
        final Settings settings = new Settings();
        settings.load();
        return !settings.equals(this);
    }

    private JSONObject getCommandsObject(TreeItem<CommandTableRow> root) throws JSONException {
        JSONObject commandsObject = new JSONObject();
        appendNodeHierarchyToJSON(root, commandsObject);
        return commandsObject;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Settings) {
            return hashCode() == other.hashCode();
        }

        return super.equals(other);
    }

    @Override
    public int hashCode() {
        try {
            int result = (haltOnError ? 1 : 0);
            result = 31 * result + (confirmNonemptyDelete ? 1 : 0);
            result = 31 * result + saveOnExit.hashCode();
            final String thisJSON;
            thisJSON = getCommandsObject(root).toString();
            result = 31 * result + thisJSON.hashCode();
            return result;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return 0;
    }
}
