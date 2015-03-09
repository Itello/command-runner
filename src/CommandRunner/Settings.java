package CommandRunner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

public class Settings {

    private static final String DIRECTORY_STRING = "directory";
    private static final String COMMAND_AND_ARGUMENTS_STRING = "commandsAndArguments";
    private static final String COMMAND_COMMENT_STRING = "commandComment";
    private static final File SAVE_FILE = new File(System.getProperty("user.home") + System.getProperty("file.separator") + ".commandRunner");
    private static final String NAME = "name";
    private static final String CHILDREN = "children";
    private static final String COMMAND = "command";

    private CommandTreeNode root = null;
    private boolean haltOnError = false;
    private boolean confirmNonemptyDelete = true;

    Settings() {

    }

    public void save(CommandTreeNode root) {
        this.root = root;

        try {
            FileOutputStream fileOut = new FileOutputStream(SAVE_FILE);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);

            JSONObject object = new JSONObject();

            appendNodeHierarchyToJSON(root, object);

            String content = object.toString();

            out.writeObject(content);
            out.writeObject(haltOnError);
            out.writeObject(confirmNonemptyDelete);
            out.close();
            fileOut.close();
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
                FileInputStream fileIn = new FileInputStream(SAVE_FILE);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                String commandContent = (String) in.readObject();

                setRoot(createNode(new JSONObject(commandContent)));
                haltOnError = (boolean) in.readObject();
                confirmNonemptyDelete = (boolean) in.readObject();

                in.close();
                fileIn.close();
            } catch (IOException | ClassNotFoundException | JSONException e) {
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

    private void setRoot(CommandTreeNode root) {
        this.root = root;
    }

    public CommandTreeNode getRoot() {
        return root;
    }
}
