package CommandRunner.gui.fxml;

import CommandRunner.CommandRunner;
import CommandRunner.Settings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    @FXML
    private CheckBox haltOnErrorCheckBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        haltOnErrorCheckBox.setSelected(CommandRunner.getInstance().getSettings().getHaltOnError());
    }

    public void save(ActionEvent event) {
        Settings settings = CommandRunner.getInstance().getSettings();
        settings.setHaltOnError(haltOnErrorCheckBox.isSelected());
        settings.save();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
