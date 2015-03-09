package CommandRunner;

import CommandRunner.gui.CommandStatus;

import java.util.*;

public class CommandQueue implements CommandListener {

    private enum CommandQueueStatus {
        Stopped,
        Running,
        Stopping
    }

    private Command runningCommand;
    private Deque<Command> commands;
    private final List<CommandQueueListener> listeners;
    private CommandQueueStatus status;

    public CommandQueue(CommandQueueListener... listeners) {
        this.listeners = new ArrayList<>(Arrays.asList(listeners));
        status = CommandQueueStatus.Stopped;
        this.commands = new ArrayDeque<>();
    }

    public void start() {
        listeners.forEach(listener -> listener.commandQueueStarted(commands.size()));
        if (status == CommandQueueStatus.Stopped) {
            status = CommandQueueStatus.Running;
            executeNextCommand();
        }
    }

    public boolean setCommands(List<Command> commands) {
        if (status == CommandQueueStatus.Stopped) {
            this.commands = new ArrayDeque<>(commands);
            this.commands.forEach(command -> command.addCommandListener(this));
            return true;
        }

        return false;
    }

    public void stopWhenCurrentCommandFinishes() {
        if (status == CommandQueueStatus.Running) {
            status = CommandQueueStatus.Stopping;
        }
    }

    public void kill() {
        runningCommand.kill();
        commands.clear();
        setStoppedState();
    }

    private void setStoppedState() {
        runningCommand = null;
        status = CommandQueueStatus.Stopped;
        listeners.forEach(CommandQueueListener::commandQueueFinished);
    }

    private void executeNextCommand() {
        final Command command = commands.pollFirst();
        if (command != null) {
            runningCommand = command;
            listeners.forEach(listener -> listener.commandQueueIsProcessing(command, commands.size() + 1));
            new Thread(command::execute).start();
        } else {
            setStoppedState();
        }
    }

    @Override
    public void commandExecuted(Command command) {
        if (status == CommandQueueStatus.Running) {
            if (command.getCommandStatus().equals(CommandStatus.FAIL) && CommandRunner.getInstance().getSettings().getHaltOnError()) {
                setStoppedState();
            } else {
                executeNextCommand();
            }
        } else if (status == CommandQueueStatus.Stopping) {
            setStoppedState();
        }
    }

    @Override
    public void commandOutput(Command command, String text) {
        // do nothing
    }
}
