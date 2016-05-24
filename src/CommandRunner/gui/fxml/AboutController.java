package CommandRunner.gui.fxml;


import CommandRunner.CommandRunner;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

@SuppressWarnings("UnusedDeclaration")
public class AboutController implements Initializable {

    @FXML
    public Label title;

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        assert title != null : "fx:id=\"title\" was not injected: check FXML file 'about .fxml'.";

        title.setText(CommandRunner.PROGRAM_TITLE + " " + CommandRunner.PROGRAM_VERSION);
    }

    public void cancel(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
