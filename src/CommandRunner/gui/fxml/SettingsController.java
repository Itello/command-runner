package CommandRunner.gui.fxml;

import CommandRunner.CommandRunner;
import CommandRunner.Settings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    @FXML
    private CheckBox haltOnErrorCheckBox;

    @FXML
    private CheckBox confirmNonemptyDeleteCheckBox;

    @FXML
    private ToggleGroup saveOnExitGroup;

    @FXML
    private RadioButton askOnExitRadioButton;

    @FXML
    private RadioButton saveOnExitRadioButton;

    @FXML
    private RadioButton forgetOnExitRadioButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert haltOnErrorCheckBox != null : "fx:id=\"haltOnErrorCheckBox\" was not injected: check FXML file 'settings.fxml'.";

        final Settings settings = CommandRunner.getInstance().getSettings();
        haltOnErrorCheckBox.setSelected(settings.getHaltOnError());
        confirmNonemptyDeleteCheckBox.setSelected(settings.getConfirmNonemptyDelete());
        saveOnExitGroup.selectToggle(getButtonFromSaveOnExitSettings(settings));
    }

    public void save(ActionEvent event) {
        Settings settings = CommandRunner.getInstance().getSettings();
        settings.setHaltOnError(haltOnErrorCheckBox.isSelected());
        settings.setConfirmNonemptyDelete(confirmNonemptyDeleteCheckBox.isSelected());
        settings.setSaveOnExit(getSaveOnExitFromToggleGroup());
        CommandRunner.getInstance().save();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private RadioButton getButtonFromSaveOnExitSettings(Settings settings) {
        Settings.SaveOnExit saveOnExit = settings.getSaveOnExit();

        switch (saveOnExit) {
            case ASK:
                return askOnExitRadioButton;
            case SAVE:
                return saveOnExitRadioButton;
            case FORGET:
                return forgetOnExitRadioButton;
            default: return null;
        }
    }

    private Settings.SaveOnExit getSaveOnExitFromToggleGroup() {
        Toggle selectedButton = saveOnExitGroup.getSelectedToggle();
        if (selectedButton == askOnExitRadioButton) {
            return Settings.SaveOnExit.ASK;
        } else if (selectedButton == saveOnExitRadioButton) {
            return Settings.SaveOnExit.SAVE;
        } else if (selectedButton == forgetOnExitRadioButton) {
            return Settings.SaveOnExit.FORGET;
        }

        return null;
    }
}
