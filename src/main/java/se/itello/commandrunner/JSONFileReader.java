package se.itello.commandrunner;

import se.itello.commandrunner.gui.commandtable.CommandTableCommandRow;
import se.itello.commandrunner.gui.commandtable.CommandTableGroupRow;
import se.itello.commandrunner.gui.commandtable.CommandTableRow;
import javafx.scene.control.TreeItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JSONFileReader {
    static final String COMMAND_AND_ARGUMENTS_STRING = "commandsAndArguments";
    static final String COMMAND_COMMENT_STRING = "commandComment";
    static final String NAME = "name";
    static final String CHILDREN = "children";
    static final String COMMAND = "command";
    static final String DIRECTORY_STRING = "directory";
    static final String IS_EXPANDED = "isExpanded";

    public static String readJsonObjectFromFile(File file) {
        final StringBuilder fileContents = new StringBuilder();
        try {
            final java.io.FileReader reader = new java.io.FileReader(file);
            int i;
            while ((i = reader.read()) != -1) {
                char ch = (char) i;

                fileContents.append(ch);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileContents.toString();
    }

    static TreeItem<CommandTableRow> createNode(JSONObject object) throws JSONException {
        JSONArray jsonChildren = object.has(CHILDREN) ? (JSONArray) object.get(CHILDREN) : null;
        JSONObject jsonCommand = object.has(COMMAND) ? (JSONObject) object.get(COMMAND) : null;

        final CommandTableRow commandTableRow;

        if (jsonCommand != null) {
            final String directory = (String) jsonCommand.get(DIRECTORY_STRING);
            final String comment = (String) jsonCommand.get(COMMAND_COMMENT_STRING);
            final String commandNameAndArguments = (String) jsonCommand.get(COMMAND_AND_ARGUMENTS_STRING);

            commandTableRow = new CommandTableCommandRow(new Command(directory, commandNameAndArguments, comment));
        } else {
            String directory = object.has(DIRECTORY_STRING) ? object.getString(DIRECTORY_STRING) : "";
            String comment = object.has(COMMAND_COMMENT_STRING) ? object.getString(COMMAND_COMMENT_STRING) : "";
            commandTableRow = new CommandTableGroupRow(object.getString(NAME), directory, comment);
        }

        boolean isExpanded = object.has(IS_EXPANDED) && object.getBoolean(IS_EXPANDED);
        TreeItem<CommandTableRow> node = new TreeItem<>(commandTableRow);
        node.setExpanded(isExpanded);

        if (jsonChildren != null) {
            for (int i = 0; i < jsonChildren.length(); i++) {
                JSONObject jsonChild = jsonChildren.getJSONObject(i);
                TreeItem<CommandTableRow> childNode = createNode(jsonChild);
                node.getChildren().add(childNode);
            }
        }

        return node;
    }

    public static TreeItem<CommandTableRow> convertToNode(JSONObject object) throws JSONException {
        return createNode(object);
    }

    public static List<TreeItem<CommandTableRow>> createNodes(JSONArray array) throws JSONException {
        List<TreeItem<CommandTableRow>> nodes = new ArrayList<>();
        for(int i = 0; i < array.length(); i++) {
            TreeItem<CommandTableRow> node = createNode(array.getJSONObject(i));
            nodes.add(node);
        }

        return nodes;
    }
}
