package CommandRunner.gui;

import CommandRunner.SizeFormatter;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class StatusBarController {
    public void doStuff(Rectangle memoryBar, Label memoryLabel) {
        memoryBar.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if(event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                System.gc();
            }
        });


        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        memoryLabel.setTooltip(new Tooltip("memory used / total memory (max memory = " + SizeFormatter.BYTES_THREE_SIGNIFICANT.format(maxMemory) + ")"));

        Timeline updater = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    long totalMemory = runtime.totalMemory();
                    long usedMemory = totalMemory - runtime.freeMemory();

                    double percentageUsed = (double) usedMemory / maxMemory;
                    double percentageTotal = (double) totalMemory / maxMemory;

                    memoryBar.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                            new Stop(0.0, Color.WHITE),
                            new Stop(percentageUsed, Color.WHITE),
                            new Stop(percentageUsed + 0.0001, Color.DARKGRAY),
                            new Stop(percentageTotal, Color.DARKGRAY),
                            new Stop(percentageTotal + 0.0001, Color.TRANSPARENT),
                            new Stop(1, Color.TRANSPARENT)
                    ));
                    memoryLabel.setText(SizeFormatter.BYTES_THREE_SIGNIFICANT.format(usedMemory) + "/" + SizeFormatter.BYTES_THREE_SIGNIFICANT.format(totalMemory));
                })
        );

        updater.setCycleCount(Animation.INDEFINITE);
        updater.play();
    }
}
