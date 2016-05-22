package CommandRunner.gui.commandtable;

import javafx.beans.property.StringProperty;

public class CommandTableRow {

    private final StringProperty commandNameAndArguments;
    private final StringProperty commandDirectory;
    // TODO: remove status column
    private final StringProperty commandStatus;
    private final StringProperty commandComment;

    CommandTableRow(StringProperty commandNameAndArguments, StringProperty commandDirectory, StringProperty commandStatus, StringProperty commandComment) {
        this.commandNameAndArguments = commandNameAndArguments;
        this.commandDirectory = commandDirectory;
        this.commandStatus = commandStatus;
        this.commandComment = commandComment;
    }

    public StringProperty commandNameAndArgumentsProperty() {
        return commandNameAndArguments;
    }

    public void setCommandNameAndArguments(String commandNameAndArguments) {
        this.commandNameAndArguments.set(commandNameAndArguments);
    }

    public StringProperty commandDirectoryProperty() {
        return commandDirectory;
    }

    public void setCommandDirectory(String commandDirectory) {
        this.commandDirectory.set(commandDirectory);
    }

    public StringProperty commandStatusProperty() {
        return commandStatus;
    }

    public void setCommandStatus(String commandStatus) {
        this.commandStatus.set(commandStatus);
    }

    public StringProperty commandCommentProperty() {
        return commandComment;
    }

    public void setCommandComment(String commandComment) {
        this.commandComment.set(commandComment);
    }
}
