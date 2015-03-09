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

    @FXML
    private CheckBox confirmNonemptyDeleteCheckBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert haltOnErrorCheckBox != null : "fx:id=\"haltOnErrorCheckBox\" was not injected: check FXML file 'settings.fxml'.";

        haltOnErrorCheckBox.setSelected(CommandRunner.getInstance().getSettings().getHaltOnError());
        confirmNonemptyDeleteCheckBox.setSelected(CommandRunner.getInstance().getSettings().getConfirmNonemptyDelete());

    }

    public void save(ActionEvent event) {
        Settings settings = CommandRunner.getInstance().getSettings();
        settings.setHaltOnError(haltOnErrorCheckBox.isSelected());
        settings.setConfirmNonemptyDelete(confirmNonemptyDeleteCheckBox.isSelected());
        CommandRunner.getInstance().save();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
