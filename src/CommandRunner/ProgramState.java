package CommandRunner;

import CommandRunner.gui.LayoutChangedListener;
import CommandRunner.gui.WindowLayout;
import CommandRunner.gui.commandtable.CommandTableGroupRow;
import CommandRunner.gui.commandtable.CommandTableRow;
import javafx.scene.control.TreeItem;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static CommandRunner.JsonConverter.appendNodeHierarchyToJSON;
import static CommandRunner.JsonConverter.convertToJSON;

public class ProgramState implements LayoutChangedListener {
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
    private static final String WINDOW_LAYOUT = "windowLayout";

    private TreeItem<CommandTableRow> root = null;
    private boolean haltOnError = true;
    private boolean confirmNonemptyDelete = true;
    private SaveOnExit saveOnExit = SaveOnExit.ASK;
    private WindowLayout windowLayout;
    private JSONObject settingsObject;

    void saveCommands(TreeItem<CommandTableRow> root) {
        try {
            this.root = root;
            JSONObject commandsObject = getCommandsObject(root);
            settingsObject.put(COMMANDS, commandsObject);

            saveToFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveSettings() {
        try {
            settingsObject.put(HALT_ON_ERROR, haltOnError);
            settingsObject.put(CONFIRM_NONEMPTY_DELETE, confirmNonemptyDelete);
            settingsObject.put(SAVE_ON_EXIT, saveOnExit.toString());

            saveToFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void saveLayout(double width, double height) {
        try {
            windowLayout.setWindowWidth((int) width);
            windowLayout.setWindowHeight((int) height);
            settingsObject.put(WINDOW_LAYOUT, windowLayout.jsonObject());
            saveToFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveToFile() throws IOException, JSONException {
        FileWriter fileWriter = new FileWriter(SAVE_FILE);
        fileWriter.write(settingsObject.toString(2));
        fileWriter.close();
    }

    void load() {
        if (SAVE_FILE.exists()) {
            try {
                final String settingsString = JSONFileReader.readJsonObjectFromFile(SAVE_FILE);
                settingsObject = JsonConverter.convertFromJSONToObject(settingsString);

                root = JSONFileReader.createNode(settingsObject.getJSONObject(COMMANDS));

                if (settingsObject.has(HALT_ON_ERROR)) {
                    haltOnError = settingsObject.getBoolean(HALT_ON_ERROR);
                }
                if (settingsObject.has(CONFIRM_NONEMPTY_DELETE)) {
                    confirmNonemptyDelete = settingsObject.getBoolean(CONFIRM_NONEMPTY_DELETE);
                }
                if (settingsObject.has(SAVE_ON_EXIT)) {
                    saveOnExit = SaveOnExit.valueOf(settingsObject.getString(SAVE_ON_EXIT));
                }
                if (settingsObject.has(WINDOW_LAYOUT)) {
                    windowLayout = new WindowLayout(settingsObject.getJSONObject(WINDOW_LAYOUT));
                } else {
                    windowLayout = WindowLayout.DEFAULT_LAYOUT;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            settingsObject = new JSONObject();
            windowLayout = WindowLayout.DEFAULT_LAYOUT;
            saveCommands(createRoot());
        }
    }

    private TreeItem<CommandTableRow> createRoot() {
        TreeItem<CommandTableRow> root = new TreeItem<>(new CommandTableGroupRow("root", "", ""));
        root.setExpanded(true);
        return root;
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

    TreeItem<CommandTableRow> getRoot() {
        return root;
    }

    boolean hasChangesSinceLastSave() {
        final ProgramState programState = new ProgramState();
        programState.load();
        return !programState.equals(this);
    }

    private JSONObject getCommandsObject(TreeItem<CommandTableRow> root) throws JSONException {
        JSONObject commandsObject = new JSONObject();
        appendNodeHierarchyToJSON(root, commandsObject);
        return commandsObject;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ProgramState) {
            return hashCode() == other.hashCode();
        }

        return super.equals(other);
    }

    @Override
    public int hashCode() {
        int result = (haltOnError ? 1 : 0);
        result = 31 * result + (confirmNonemptyDelete ? 1 : 0);
        result = 31 * result + saveOnExit.hashCode();
        try {
            result = 31 * result + convertToJSON(root).toString().hashCode();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public void verticalDividerPositionChanged(double to) {
        windowLayout.setVerticalDividerPosition(to);
    }

    @Override
    public void horizontalDividerPositionChanged(double to) {
        windowLayout.setHorizontalDividerPosition(to);
    }

    @Override
    public void tableCommandColumnWidthChanged(int to) {
        windowLayout.setTableCommandColumnWidth(to);
    }

    @Override
    public void tableDirectoryColumnWidthChanged(int to) {
        windowLayout.setTableDirectoryColumnWidth(to);
    }

    @Override
    public void tableCommentColumnWidthChanged(int to) {
        windowLayout.setTableCommentColumnWidth(to);
    }

    WindowLayout getWindowLayout() {
        return windowLayout;
    }
}
