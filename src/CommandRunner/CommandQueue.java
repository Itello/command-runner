package CommandRunner;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;

import static CommandRunner.CommandStatus.sortCommandStatuses;

public class CommandQueue implements CommandListener {

    private final static int MAX_OUTPUT_CAPACITY = 1024;

    public enum CommandQueueStatus {
        Stopped,
        Running,
        Stopping
    }

    private final List<CommandQueueListener> listeners;
    private final Map<Command, ArrayBlockingQueue<String>> commandOutputs;

    private int commandToRunIndex;
    private List<Command> commands;

    private CommandQueueStatus status;

    CommandQueue(CommandQueueListener... listeners) {
        this.listeners = new ArrayList<>();
        if (listeners != null) {
            this.listeners.addAll(Arrays.asList(listeners));
        }

        status = CommandQueueStatus.Stopped;
        commandOutputs = new HashMap<>();
    }

    void start() {
        listeners.forEach(listener -> listener.commandQueueStarted(this));
        if (status == CommandQueueStatus.Stopped) {
            status = CommandQueueStatus.Running;
            executeNextCommand();
        }
    }

    void setCommands(List<Command> commands) {
        if (status == CommandQueueStatus.Stopped) {
            this.commands = new ArrayList<>(commands);
            commandToRunIndex = 0;
            this.commands.forEach(command -> command.addCommandListener(this));
        }
    }

    public void stopWhenCurrentCommandFinishes() {
        if (status == CommandQueueStatus.Running) {
            status = CommandQueueStatus.Stopping;
        }
    }

    public void kill() {
        stopWhenCurrentCommandFinishes();
        getCurrentCommand().ifPresent(command -> command.kill());
    }

    private Optional<Command> getCurrentCommand() {
        if (commandToRunIndex < commands.size()) {
            return Optional.of(commands.get(commandToRunIndex));
        }

        return Optional.empty();
    }

    private void setStoppedState() {
        status = CommandQueueStatus.Stopped;
    }

    private void executeNextCommand() {
        Optional<Command> command = getCurrentCommand();
        if (command.isPresent()) {
            new Thread(command.get()::execute).start();
            listeners.forEach(listener -> listener.commandQueueIsProcessing(command.get()));
        } else {
            setStoppedState();
            listeners.forEach(listener -> listener.commandQueueFinished(this));
        }
    }

    @Override
    public void commandExecuted(Command command) {
        if (status == CommandQueueStatus.Running) {
            if (command.getCommandStatus().equals(CommandStatus.FAIL) && CommandRunner.getInstance().getProgramState().getHaltOnError()) {
                setStoppedState();
                listeners.forEach(listener -> listener.commandQueueFinished(this));
            } else {
                commandToRunIndex++;
                executeNextCommand();
            }
        } else if (status == CommandQueueStatus.Stopping) {
            setStoppedState();
            listeners.forEach(listener -> listener.commandQueueFinished(this));
        }
    }

    @Override
    public void commandOutput(Command command, String text) {
        ArrayBlockingQueue<String> outputForCommand = getCommandOutput(command);
        if (outputForCommand.size() >= MAX_OUTPUT_CAPACITY) {
            try {
                outputForCommand.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        outputForCommand.add(text);
    }

    private ArrayBlockingQueue<String> getCommandOutput(Command command) {
        ArrayBlockingQueue<String> outputForCommand = commandOutputs.get(command);
        if (outputForCommand == null) {
            outputForCommand = new ArrayBlockingQueue<>(MAX_OUTPUT_CAPACITY);
            commandOutputs.put(command, outputForCommand);
        }
        return outputForCommand;
    }

    public ArrayBlockingQueue<String> getCommandOutput() {
        return commands.stream()
                .map(this::getCommandOutput)
                .reduce(new ArrayBlockingQueue<>(MAX_OUTPUT_CAPACITY), (a, b) -> {
                    a.addAll(b);
                    return a;
                });
    }

    public List<Command> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public CommandStatus getCommandStatus() {
        List<CommandStatus> commandStatusList = commands.stream()
                .map(Command::getCommandStatus)
                .collect(Collectors.toList());

        sortCommandStatuses(commandStatusList);
        CommandStatus commandStatus = commandStatusList.get(0);
        if (commandStatus == CommandStatus.IDLE && this.status == CommandQueueStatus.Running) {
            // case where we started queue but command didn't have time to set up yet
            return CommandStatus.RUNNING;
        }
        return commandStatus;
    }

    private boolean isCommandInCommandQueue(Command command) {
        return commands.contains(command);
    }

    public ArrayBlockingQueue<String> getCommandOutputForCommand(Command command) {
        if (isCommandInCommandQueue(command)) {
            return getCommandOutput(command);
        }

        throw new IllegalArgumentException("command not in command queue!");
    }

    public CommandQueueStatus getQueueStatus() {
        return status;
    }
}
