package CommandRunner;

import CommandRunner.gui.CommandStatus;

import java.util.*;
import java.util.stream.Collectors;

public class CommandQueue implements CommandListener {

    private enum CommandQueueStatus {
        Stopped,
        Running,
        Stopping
    }

    private final List<CommandQueueListener> listeners;
    private final Map<Command, List<String>> commandOutputs;

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
        getCurrentCommand().ifPresent(command -> command.kill());
        commands.clear();
        setStoppedState();
    }

    private Optional<Command> getCurrentCommand() {
        if (commandToRunIndex < commands.size()) {
            return Optional.of(commands.get(commandToRunIndex));
        }

        return Optional.empty();
    }

    private void setStoppedState() {
        status = CommandQueueStatus.Stopped;
        listeners.forEach(listener -> listener.commandQueueFinished(this));
    }

    private void executeNextCommand() {
        Optional<Command> command = getCurrentCommand();
        if (command.isPresent()) {
            listeners.forEach(listener -> listener.commandQueueIsProcessing(command.get()));
            commandToRunIndex++;
            new Thread(command.get()::execute).start();
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
        List<String> outputForCommand = getCommandOutput(command);

        outputForCommand.add(text);
    }

    private List<String> getCommandOutput(Command command) {
        List<String> outputForCommand = commandOutputs.get(command);
        if (outputForCommand == null) {
            outputForCommand = new ArrayList<>();
            commandOutputs.put(command, outputForCommand);
        }
        return outputForCommand;
    }

    public List<String> getCommandOutput() {
        return commandOutputs.values().stream()
                .reduce(new ArrayList<>(), (a, b) -> {
                            a.addAll(b);
                            return a;
                        }
                );
    }

    public List<Command> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public List<String> getCommandNameAndArguments() {
        return commands.stream()
                .map(command -> command.getCommandNameAndArguments())
                .collect(Collectors.toList());
    }

    public CommandStatus getCommandStatus() {
        return commands.stream()
                .map(Command::getCommandStatus)
                .sorted((s1, s2) -> {
                    boolean firstFail = s1.equals(CommandStatus.FAIL);
                    boolean firstIdle = s1.equals(CommandStatus.IDLE);
                    boolean secondFail = s1.equals(CommandStatus.FAIL);
                    boolean secondIdle = s1.equals(CommandStatus.IDLE);
                    if (firstFail && !secondFail) {
                        return -1;
                    } else if (secondFail && !firstFail) {
                        return 1;
                    } else if (firstIdle && !secondIdle) {
                        return -1;
                    } else if (secondIdle && !firstIdle) {
                        return 1;
                    }

                    return 0;
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Queue without commands"));
    }

    private boolean isCommandInCommandQueue(Command command) {
        return commands.contains(command);
    }

    public List<String> getCommandOutputForCommand(Command command) {
        if (isCommandInCommandQueue(command)) {
            return getCommandOutput(command);
        }

        throw new IllegalArgumentException("command not in command queue!");
    }
}
