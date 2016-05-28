package CommandRunner.gui.commandqueuetree;

import CommandRunner.*;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
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
    private final LimitTextArea commandOutputArea;

    private TextAppendThread appendThread;
    private int runningQueues = 0;

    public CommandQueueTreeController(TreeView<CommandQueueTreeRow> commandQueueTreeView, LimitTextArea commandOutputArea) {
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
        boolean running = false;
        for (TreeItem<CommandQueueTreeRow> commandQueueTreeItemTreeItem : selection) {
            if (commandQueueTreeItemTreeItem == null) {
                continue;
            }

            CommandQueueTreeRow item = commandQueueTreeItemTreeItem.getValue();
            if (item instanceof CommandQueueTreeCommandQueueRow) {
                CommandQueue commandQueue = ((CommandQueueTreeCommandQueueRow) item).getCommandQueue();
                commandQueue.getCommandOutput().forEach(line -> sb.append(line).append('\n'));
                running = running || commandQueue.getQueueStatus() == CommandQueue.CommandQueueStatus.Running;
            } else if (item instanceof CommandQueueTreeCommandRow) {
                Command command = ((CommandQueueTreeCommandRow) item).getCommand();
                CommandQueue commandQueue = ((CommandQueueTreeCommandRow) item).getCommandQueue();
                commandQueue.getCommandOutputForCommand(command).forEach(line -> sb.append(line).append('\n'));
                running = running || commandQueue.getCommandStatus() == CommandStatus.RUNNING;
            }
        }

        if (running) {
            startAppendThread();
        } else {
            stopAppendThreadIfRunning();
        }

        commandOutputArea.setTextLimited(sb.toString());
    }

    @Override
    public void commandQueueStarted(CommandQueue commandQueue) {
        commandOutputArea.clear();
        runningQueues++;

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
        startAppendThread();
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
        if (--runningQueues <= 0) {
            stopAppendThreadIfRunning();
        }

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
        getTreeItemForCommand(commandQueueTreeView.getRoot(), command)
                .ifPresent(treeItem -> setItemGraphic(treeItem, RUNNING_COMMAND_GRAPHIC));

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

    private void startAppendThread() {
        stopAppendThreadIfRunning();
        appendThread = new TextAppendThread(commandOutputArea);
        appendThread.setDaemon(true);
        appendThread.start();
    }

    private void stopAppendThreadIfRunning() {
        if (appendThread != null) {
            appendThread.setDone();
            appendThread.setTextAreaToNull();
        }
    }

    @Override
    public void commandOutput(Command command, String output) {
        if (isCommandSelected(command)) {
            appendThread.add(output + '\n');
        }
    }

    private boolean isCommandSelected(Command command) {
        return selectedCommandRows().anyMatch(row -> row.getCommand().equals(command)) ||
                selectedCommandQueueRows().anyMatch(row -> row.getCommandQueue().getCommands().contains(command));
    }

    private Stream<CommandQueueTreeCommandRow> selectedCommandRows() {
        return commandQueueTreeView.getSelectionModel().getSelectedItems().stream()
                .filter(item -> item != null)
                .map(TreeItem::getValue)
                .filter(row -> row instanceof CommandQueueTreeCommandRow)
                .map(row -> (CommandQueueTreeCommandRow) row);
    }

    private Stream<CommandQueueTreeCommandQueueRow> selectedCommandQueueRows() {
        return commandQueueTreeView.getSelectionModel().getSelectedItems().stream()
                .filter(item -> item != null)
                .map(TreeItem::getValue)
                .filter(row -> row instanceof CommandQueueTreeCommandQueueRow)
                .map(row -> (CommandQueueTreeCommandQueueRow) row);
    }

    public void killSelected() {
        selectedCommandRows().forEach(row -> row.getCommand().kill());
        selectedCommandQueueRows().forEach(row -> row.getCommandQueue().kill());
    }

    public void stopSelected() {
        selectedCommandQueueRows().forEach(row -> row.getCommandQueue().stopWhenCurrentCommandFinishes());
    }

    public void clearQueue() {
        commandQueueTreeView.getRoot().getChildren().clear();
        commandOutputArea.clear();
    }

    public void sendInput(String text) {
        selectedCommandRows()
                .map(CommandQueueTreeCommandRow::getCommand)
                .filter(command -> command.getCommandStatus().equals(CommandStatus.RUNNING))
                .forEach(command -> command.sendInput(text));
    }

    public void stopAppendingText() {
        stopAppendThreadIfRunning();
    }
}
