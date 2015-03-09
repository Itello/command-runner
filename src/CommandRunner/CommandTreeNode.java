package CommandRunner;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CommandTreeNode implements Serializable {
    private final String name;
    private final List<CommandTreeNode> children;
    private final Command command;

    public CommandTreeNode(String name, Command command) {
        this.name = name;
        this.command = command;

        if (command == null) {
            children = new ArrayList<>();
        } else {
            children = null;
        }
    }

    public Command getCommand() {
        return command;
    }

    public String getName() {
        if (hasCommand()) {
            return getCommand().getCommandNameAndArguments();
        }

        return name;
    }

    public List<CommandTreeNode> getChildren() {
        return children;
    }

    public boolean hasCommand() {
        return command != null;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    public void addChild(CommandTreeNode child) {
        children.add(child);
    }
}
