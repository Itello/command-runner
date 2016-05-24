package CommandRunner.gui.fxml;

import CommandRunner.CommandRunner;
import CommandRunner.ProgramState;
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

        final ProgramState programState = CommandRunner.getInstance().getProgramState();
        haltOnErrorCheckBox.setSelected(programState.getHaltOnError());
        confirmNonemptyDeleteCheckBox.setSelected(programState.getConfirmNonemptyDelete());
        saveOnExitGroup.selectToggle(getButtonFromSaveOnExitSettings(programState));
    }

    public void save(ActionEvent event) {
        ProgramState programState = CommandRunner.getInstance().getProgramState();
        programState.setHaltOnError(haltOnErrorCheckBox.isSelected());
        programState.setConfirmNonemptyDelete(confirmNonemptyDeleteCheckBox.isSelected());
        programState.setSaveOnExit(getSaveOnExitFromToggleGroup());
        programState.saveSettings();
        closeStage(event);
    }

    private RadioButton getButtonFromSaveOnExitSettings(ProgramState programState) {
        ProgramState.SaveOnExit saveOnExit = programState.getSaveOnExit();

        switch (saveOnExit) {
            case ASK:
                return askOnExitRadioButton;
            case SAVE:
                return saveOnExitRadioButton;
            case FORGET:
                return forgetOnExitRadioButton;
            default:
                return null;
        }
    }

    private ProgramState.SaveOnExit getSaveOnExitFromToggleGroup() {
        Toggle selectedButton = saveOnExitGroup.getSelectedToggle();
        if (selectedButton == askOnExitRadioButton) {
            return ProgramState.SaveOnExit.ASK;
        } else if (selectedButton == saveOnExitRadioButton) {
            return ProgramState.SaveOnExit.SAVE;
        } else if (selectedButton == forgetOnExitRadioButton) {
            return ProgramState.SaveOnExit.FORGET;
        }

        return null;
    }

    public void cancel(ActionEvent event) {
        closeStage(event);
    }

    private void closeStage(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
