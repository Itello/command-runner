package CommandRunner.gui.commandqueuetree;

import CommandRunner.Command;
import CommandRunner.CommandListener;
import CommandRunner.CommandQueue;
import CommandRunner.CommandQueueListener;
import CommandRunner.gui.CommandStatus;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.util.Optional;
import java.util.stream.Stream;

public class CommandQueueTreeController implements CommandListener, CommandQueueListener {
    private static final Image IDLE_COMMAND_GRAPHIC = new Image("png/pause.png");
    private static final Image RUNNING_COMMAND_GRAPHIC = new Image("png/running.png");
    private static final Image DONE_COMMAND_GRAPHIC = new Image("png/done.png");
    private static final Image FAIL_COMMAND_GRAPHIC = new Image("png/fail.png");

    private final TreeView<CommandQueueTreeRow> commandQueueTreeView;
    private final TextArea commandOutputArea;

    public CommandQueueTreeController(TreeView<CommandQueueTreeRow> commandQueueTreeView, TextArea commandOutputArea) {
        this.commandQueueTreeView = commandQueueTreeView;
        this.commandOutputArea = commandOutputArea;
        commandQueueTreeView.setCellFactory(p -> new CommandQueueTreeCell());
        commandQueueTreeView.setShowRoot(false);
        commandQueueTreeView.setRoot(new TreeItem<>());
        commandQueueTreeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        commandQueueTreeView.getSelectionModel().getSelectedItems().addListener(
                (ListChangeListener<? super TreeItem<CommandQueueTreeRow>>) listener -> selectionUpdated(listener.getList())
        );
    }

    private void selectionUpdated(ObservableList<? extends TreeItem<CommandQueueTreeRow>> selection) {
        StringBuilder sb = new StringBuilder();
        for (TreeItem<CommandQueueTreeRow> commandQueueTreeItemTreeItem : selection) {
            CommandQueueTreeRow item = commandQueueTreeItemTreeItem.getValue();
            if (item instanceof CommandQueueTreeCommandQueueRow) {
                CommandQueue commandQueue = ((CommandQueueTreeCommandQueueRow) item).getCommandQueue();
                commandQueue.getCommandOutput().forEach(line -> sb.append(line).append('\n'));
            } else if (item instanceof CommandQueueTreeCommandRow) {
                Command command = ((CommandQueueTreeCommandRow) item).getCommand();
                CommandQueue commandQueue = ((CommandQueueTreeCommandRow) item).getCommandQueue();
                commandQueue.getCommandOutputForCommand(command).forEach(line -> sb.append(line).append('\n'));
            }
        }

        commandOutputArea.setText(sb.toString());
    }

    @Override
    public void commandQueueStarted(CommandQueue commandQueue) {
        commandOutputArea.clear();

        final TreeItem<CommandQueueTreeRow> treeItem;
        if (commandQueue.getCommands().size() > 1) {
            String name = "" + commandQueueTreeView.getRoot().getChildren().size();
            treeItem = new TreeItem<>(new CommandQueueTreeCommandQueueRow(commandQueue, name));
            commandQueue.getCommands().forEach(command -> {
                TreeItem<CommandQueueTreeRow> child = new TreeItem<>(new CommandQueueTreeCommandRow(command, commandQueue));
                setItemGraphic(child, IDLE_COMMAND_GRAPHIC);
                treeItem.getChildren().add(child);
            });
            treeItem.setExpanded(true);
        } else {
            Command command = commandQueue.getCommands().get(0);
            treeItem = new TreeItem<>(new CommandQueueTreeCommandRow(command, commandQueue));
        }

        setItemGraphic(treeItem, RUNNING_COMMAND_GRAPHIC);
        commandQueueTreeView.getRoot().getChildren().add(0, treeItem);
        commandQueueTreeView.getSelectionModel().clearAndSelect(0);
    }

    private void setItemGraphic(TreeItem<CommandQueueTreeRow> treeItem, Image image) {
        ImageView imageView = new ImageView(image);
        if (image == RUNNING_COMMAND_GRAPHIC) {
            addRotation(imageView);
        }
        treeItem.setGraphic(imageView);
    }

    private void addRotation(ImageView imageView) {
        RotateTransition rt = new RotateTransition(Duration.millis(1000), imageView);
        rt.setToAngle(360);
        rt.setCycleCount(Animation.INDEFINITE);
        rt.setInterpolator(Interpolator.LINEAR);
        rt.play();
    }

    @Override
    public void commandQueueFinished(CommandQueue commandQueue) {
        getTreeItemForQueue(commandQueue)
                .ifPresent(treeItem -> updateGraphic(commandQueue.getCommandStatus(), treeItem));
    }

    private void updateGraphic(CommandStatus commandStatus, TreeItem<CommandQueueTreeRow> treeItemForQueue) {
        switch (commandStatus) {
            case OK:
                treeItemForQueue.setGraphic(new ImageView(DONE_COMMAND_GRAPHIC));
                break;
            case FAIL:
                treeItemForQueue.setGraphic(new ImageView(FAIL_COMMAND_GRAPHIC));
                break;
            case IDLE:
                treeItemForQueue.setGraphic(new ImageView(IDLE_COMMAND_GRAPHIC));
                break;
            case RUNNING:
            default:
                throw new IllegalStateException("Undefined behavior");
        }

        commandQueueTreeView.refresh();
    }

    private Optional<TreeItem<CommandQueueTreeRow>> getTreeItemForQueue(CommandQueue commandQueue) {
        for (TreeItem<CommandQueueTreeRow> commandQueueTreeItemTreeItem : commandQueueTreeView.getRoot().getChildren()) {
            CommandQueueTreeRow value = commandQueueTreeItemTreeItem.getValue();
            if (value instanceof CommandQueueTreeCommandQueueRow
                    && ((CommandQueueTreeCommandQueueRow) value).getCommandQueue().equals(commandQueue)) {
                return Optional.of(commandQueueTreeItemTreeItem);
            }
        }

        return Optional.empty();
    }

    @Override
    public void commandQueueIsProcessing(Command command) {
        getTreeItemForCommand(commandQueueTreeView.getRoot(), command).ifPresent(treeItem -> {
            setItemGraphic(treeItem, RUNNING_COMMAND_GRAPHIC);
        });

        command.addCommandListener(this);
    }

    @Override
    public void commandExecuted(Command command) {
        getTreeItemForCommand(commandQueueTreeView.getRoot(), command)
                .ifPresent(treeItem -> updateGraphic(command.getCommandStatus(), treeItem));
    }

    private Optional<TreeItem<CommandQueueTreeRow>> getTreeItemForCommand(TreeItem<CommandQueueTreeRow> root, Command command) {
        for (TreeItem<CommandQueueTreeRow> treeItem : root.getChildren()) {
            CommandQueueTreeRow value = treeItem.getValue();
            if (value instanceof CommandQueueTreeCommandRow
                    && command == ((CommandQueueTreeCommandRow) value).getCommand()) {
                return Optional.of(treeItem);
            }

            Optional<TreeItem<CommandQueueTreeRow>> childTreeItem = getTreeItemForCommand(treeItem, command);
            if (childTreeItem.isPresent()) {
                return childTreeItem;
            }
        }
        return Optional.empty();
    }

    @Override
    public void commandOutput(Command command, String text) {
        if (isCommandSelected(command)) {
            commandOutputArea.appendText(text + "\n");
        }
    }

    private boolean isCommandSelected(Command command) {
        return selectedAndRunningCommandRows().anyMatch(row -> row.getCommand().equals(command)) ||
                selectedAndRunningCommandQueueRows().anyMatch(row -> row.getCommandQueue().getCommands().contains(command));
    }

    private Stream<CommandQueueTreeCommandRow> selectedAndRunningCommandRows() {
        return commandQueueTreeView.getSelectionModel().getSelectedItems().stream()
                .filter(item -> item != null)
                .map(TreeItem::getValue)
                .filter(row -> row instanceof CommandQueueTreeCommandRow)
                .map(row -> (CommandQueueTreeCommandRow) row)
                .filter(row -> row.getCommand().getCommandStatus().equals(CommandStatus.RUNNING));
    }

    private Stream<CommandQueueTreeCommandQueueRow> selectedAndRunningCommandQueueRows() {
        return commandQueueTreeView.getSelectionModel().getSelectedItems().stream()
                .filter(item -> item != null)
                .map(TreeItem::getValue)
                .filter(row -> row instanceof CommandQueueTreeCommandQueueRow)
                .map(row -> (CommandQueueTreeCommandQueueRow) row)
                .filter(row -> row.getCommandQueue().getCommandStatus().equals(CommandStatus.RUNNING));
    }

    public void killSelected() {
        selectedAndRunningCommandRows().forEach(row -> row.getCommand().kill());
        selectedAndRunningCommandQueueRows().forEach(row -> row.getCommandQueue().kill());
    }

    public void stopSelected() {
        selectedAndRunningCommandQueueRows().forEach(row -> row.getCommandQueue().stopWhenCurrentCommandFinishes());
    }

    public void clearQueue() {
        commandQueueTreeView.getRoot().getChildren().clear();
        commandOutputArea.clear();
    }
}
