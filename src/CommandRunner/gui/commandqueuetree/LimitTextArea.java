package CommandRunner.gui.commandqueuetree;

import javafx.scene.control.TextArea;

public class LimitTextArea extends TextArea {
    private static final int MAX_LENGTH = 102400;

    void appendTextLimited(String text) {
        int length = getLength();

        if (length > MAX_LENGTH) {
            deleteText(0, length - MAX_LENGTH);
        }

        super.appendText(text);
    }

    void setTextLimited(String text) {
        int textLength = text.length();
        if (textLength > MAX_LENGTH) {
            super.setText(text.substring(textLength - MAX_LENGTH, textLength));
        }

        super.setText(text);
        super.appendText("");
    }
}
