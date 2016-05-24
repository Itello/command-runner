package CommandRunner;

import CommandRunner.gui.commandtable.CommandTableCommandRow;
import CommandRunner.gui.commandtable.CommandTableRow;
import javafx.scene.control.TreeItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class JsonConverter {
    public static JSONObject convertToJSON(TreeItem<CommandTableRow> root) throws JSONException {
        JSONObject commandsObject = new JSONObject();
        appendNodeHierarchyToJSON(root, commandsObject);

        return commandsObject;
    }

    public static JSONArray convertToJSON(List<TreeItem<CommandTableRow>> items) throws JSONException {
        JSONArray array = new JSONArray();

        for (TreeItem<CommandTableRow> item : items) {
            JSONObject commandsObject = new JSONObject();
            appendNodeHierarchyToJSON(item, commandsObject);
            array.put(commandsObject);
        }

        return array;
    }

    static void appendNodeHierarchyToJSON(TreeItem<CommandTableRow> node, JSONObject object) throws JSONException {
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
            object.put(JSONFileReader.COMMAND_COMMENT_STRING, commandTableRow.commandCommentProperty().getValue());
            object.put(JSONFileReader.DIRECTORY_STRING, commandTableRow.commandDirectoryProperty().getValue());
            object.put(JSONFileReader.IS_EXPANDED, node.isExpanded());
            JSONArray array = new JSONArray();
            for (TreeItem<CommandTableRow> child : node.getChildren()) {
                JSONObject childJSON = new JSONObject();
                appendNodeHierarchyToJSON(child, childJSON);
                array.put(childJSON);
            }
            object.put(JSONFileReader.CHILDREN, array);
        }
    }

    public static JSONArray convertFromJSONToArray(String jsonFromFile) throws JSONException {
        return new JSONArray(jsonFromFile);
    }

    public static JSONObject convertFromJSONToObject(String jsonFromFile) throws JSONException {
        return new JSONObject(jsonFromFile);
    }
}
