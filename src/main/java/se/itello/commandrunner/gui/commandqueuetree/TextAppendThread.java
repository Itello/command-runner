package se.itello.commandrunner.gui.commandqueuetree;

import javafx.application.Platform;

import java.util.concurrent.atomic.AtomicReference;

class TextAppendThread extends Thread {
    private StringBuilder outPutStrings;
    private final AtomicReference<StringBuilder> reference;
    private LimitTextArea commandOutputArea;
    private boolean done;
    private boolean clearing;

    TextAppendThread(LimitTextArea commandOutputArea) {
        this.commandOutputArea = commandOutputArea;
        outPutStrings = new StringBuilder();
        reference = new AtomicReference<>(null);
    }

    @Override
    public void run() {
        while (!done) {
            if (reference.getAndSet(outPutStrings) == null) {
                synchronized (this) {
                    while (clearing) {
                        microSleep();
                    }
                    clearing = true;
                    updateGui();
                    outPutStrings = new StringBuilder();
                }
                clearing = false;
            }
        }
    }

    private synchronized void updateGui() {
        Platform.runLater(() -> {
            StringBuilder outputStringsToPrint = reference.getAndSet(null);
            if (commandOutputArea != null && outputStringsToPrint.length() > 0) {
                commandOutputArea.appendTextLimited(outputStringsToPrint.toString());
            }
        });
    }

    void setDone() {
        this.done = true;
    }

    public synchronized void add(String string) {
        while (clearing) {
            microSleep();
        }
        outPutStrings.append(string);
    }

    private void microSleep() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void setTextAreaToNull() {
        this.commandOutputArea = null;
    }
}