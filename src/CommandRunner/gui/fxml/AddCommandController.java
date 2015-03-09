package CommandRunner.gui.fxml;

import CommandRunner.CommandRunner;
import CommandRunner.gui.CommandController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

@SuppressWarnings("UnusedDeclaration")
public class AddCommandController implements Initializable, CommandController {

    @FXML
    private TextField directoryTexTField;

    @FXML
    private TextField nameAndArgumentsTextField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert directoryTexTField != null : "fx:id=\"directoryTexTField\" was not injected: check FXML file 'addCommand.fxml'.";
    }

    @Override
    public String getCommandDirectory() {
        return directoryTexTField.getText();
    }

    @Override
    public String getCommandNameAndArguments() {
        return nameAndArgumentsTextField.getText();
    }

    @FXML
    private void browseForDirectory(ActionEvent event) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("Command directory or command 'start in' directory");

        File directory = fileChooser.showDialog(CommandRunner.getInstance().getPrimaryStage());

        if (directory != null && directory.exists()) {
            directoryTexTField.setText(directory.getAbsolutePath());
        }
    }
}
