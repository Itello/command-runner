package CommandRunner;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Settings {

    private static final String DIRECTORY_STRING = "directory";
    private static final String COMMAND_AND_ARGUMENTS_STRING = "commandsAndArguments";
    private static final String COMMAND_COMMENT_STRING = "commandComment";
    private static final File SAVE_FILE = new File(System.getProperty("user.home") + System.getProperty("file.separator") + ".commandRunner");

    private List<Command> commands;
    private boolean haltOnError;

    Settings() {
        commands = new ArrayList<>();
    }

    public void save() {
        try {
            FileOutputStream fileOut = new FileOutputStream(SAVE_FILE);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);

            String content = String.join("\n", commands.stream()
                    .map(this::convertCommandToKeyValueString)
                    .collect(Collectors.toList()));

            out.writeObject(content);
            out.writeObject(haltOnError);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    void load() {
        if (SAVE_FILE.exists()) {
            try {
                FileInputStream fileIn = new FileInputStream(SAVE_FILE);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                String commandContent = (String) in.readObject();
                loadCommandsFromContent(commandContent);

                haltOnError = (boolean) in.readObject();
                in.close();
                fileIn.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadCommandsFromContent(String commandContent) {
        final String[] split = commandContent.split("\n");
        Arrays.asList(split).forEach(
                line -> {
                    Map<String, String> map = new HashMap<>();
                    for (String keyValuePair : line.split(";")) {
                        String[] keyValue = keyValuePair.split("=");
                        String key = keyValue[0];
                        String value = "";
                        if (keyValue.length > 1) {
                            value = keyValue[1];
                        }
                        map.put(key, value);
                    }
                    commands.add(new Command(map.get(DIRECTORY_STRING), map.get(COMMAND_AND_ARGUMENTS_STRING), map.get(COMMAND_COMMENT_STRING)));
                }
        );
    }

    public void setHaltOnError(boolean halt) {
        haltOnError = halt;
    }

    void setCommands(List<Command> commands) {
        this.commands = commands;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public boolean getHaltOnError() {
        return haltOnError;
    }

    private String convertCommandToKeyValueString(Command command) {
        return DIRECTORY_STRING + "=" + command.getCommandDirectory() + ";"
                + COMMAND_AND_ARGUMENTS_STRING + "=" + command.getCommandNameAndArguments() + ";"
                + COMMAND_COMMENT_STRING + "=" + command.getCommandComment();
    }
}
