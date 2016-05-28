package CommandRunner.gui.commandqueuetree;

import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

class TextAppendThread extends Thread {
    private List<String> strings;
    private final AtomicReference<List<String>> reference;
    private LimitTextArea commandOutputArea;
    private boolean done;

    TextAppendThread(LimitTextArea commandOutputArea) {
        this.commandOutputArea = commandOutputArea;
        strings = new ArrayList<>();
        reference = new AtomicReference<>(null);
    }

    @Override
    public void run() {
        while (!done) {
            if (reference.getAndSet(strings) == null) {
                updateGui();
            }
        }
    }

    private void updateGui() {
        Platform.runLater(() -> {
            List<String> stranger = reference.getAndSet(null);
            strings = new ArrayList<>();
            if (commandOutputArea != null) {
                String join = String.join("", stranger);
                commandOutputArea.appendTextLimited(join);
            }
        });
    }

    void setDone() {
        this.done = true;
    }

    public void add(String string) {
        strings.add(string);
    }

    void setTextAreaToNull() {
        this.commandOutputArea = null;
    }
}