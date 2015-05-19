package CommandRunner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Settings {

    public enum SaveOnExit {
        ASK,
        SAVE,
        FORGET
    }

    private static final String DIRECTORY_STRING = "directory";

    private static final File SAVE_FILE = new File(System.getProperty("user.home") + System.getProperty("file.separator") + ".commandRunner");
    private static final String COMMAND_AND_ARGUMENTS_STRING = "commandsAndArguments";
    private static final String COMMAND_COMMENT_STRING = "commandComment";
    private static final String NAME = "name";
    private static final String CHILDREN = "children";
    private static final String COMMAND = "command";
    private static final String COMMANDS = "commands";
    private static final String HALT_ON_ERROR = "haltOnError";
    private static final String CONFIRM_NONEMPTY_DELETE = "confirmNonemptyDelete";
    private static final String SAVE_ON_EXIT = "saveOnExit";

    private CommandTreeNode root = null;
    private boolean haltOnError = true;
    private boolean confirmNonemptyDelete = true;
    private SaveOnExit saveOnExit = SaveOnExit.ASK;

    Settings() {

    }

    public void save(CommandTreeNode root) {
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

    private void appendNodeHierarchyToJSON(CommandTreeNode node, JSONObject object) throws JSONException {
        if (node.hasCommand()) {
            Command command = node.getCommand();
            JSONObject commandObject = new JSONObject();
            commandObject.put(DIRECTORY_STRING, command.getCommandDirectory());
            commandObject.put(COMMAND_AND_ARGUMENTS_STRING, command.getCommandNameAndArguments());
            commandObject.put(COMMAND_COMMENT_STRING, command.getCommandComment());
            object.put(COMMAND, commandObject);
        } else {
            object.put(NAME, node.getName());
        }

        if (node.hasChildren()) {
            JSONArray array = new JSONArray();
            for (CommandTreeNode child : node.getChildren()) {
                JSONObject childJSON = new JSONObject();
                appendNodeHierarchyToJSON(child, childJSON);
                array.put(childJSON);
            }
            object.put(CHILDREN, array);
        }
    }

    private CommandTreeNode createNode(JSONObject object) throws JSONException {
        Command command = null;
        JSONArray jsonChildren = object.has(CHILDREN) ? (JSONArray) object.get(CHILDREN) : null;
        JSONObject jsonCommand = object.has(COMMAND) ? (JSONObject) object.get(COMMAND) : null;
        String name = null;

        if (jsonCommand != null) {
            final String directory = (String) jsonCommand.get(DIRECTORY_STRING);
            final String comment = (String) jsonCommand.get(COMMAND_COMMENT_STRING);
            final String commandNameAndArguments = (String) jsonCommand.get(COMMAND_AND_ARGUMENTS_STRING);

            command = new Command(directory, commandNameAndArguments, comment);
        } else {
            name = (String) object.get(NAME);
        }

        CommandTreeNode node = new CommandTreeNode(name, command);

        if (jsonChildren != null) {
            for (int i = 0; i < jsonChildren.length(); i++) {
                JSONObject jsonChild = jsonChildren.getJSONObject(i);
                CommandTreeNode childNode = createNode(jsonChild);
                node.addChild(childNode);
            }
        }

        return node;
    }

    void load() {
        if (SAVE_FILE.exists()) {
            try {
                final FileReader reader = new FileReader(SAVE_FILE);
                final StringBuilder fileContents = new StringBuilder();

                int i;
                while ((i = reader.read()) != -1) {
                    char ch = (char) i;

                    fileContents.append(ch);
                }
                final JSONObject settingsObject = new JSONObject(fileContents.toString());
                setRoot(createNode(settingsObject.getJSONObject(COMMANDS)));
                haltOnError = settingsObject.getBoolean(HALT_ON_ERROR);
                confirmNonemptyDelete = settingsObject.getBoolean(CONFIRM_NONEMPTY_DELETE);
                saveOnExit = SaveOnExit.valueOf(settingsObject.getString(SAVE_ON_EXIT));

                reader.close();
            } catch (IOException | JSONException e) {
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

    private void setRoot(CommandTreeNode root) {
        this.root = root;
    }

    public CommandTreeNode getRoot() {
        return root;
    }
}
