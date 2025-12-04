package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Exam;
import service.WordExporter;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the "Hinweise" (Instructions) dialog, which allows the user
 * to configure general exam instructions, allowed aids (Hilfsmittel) using
 * checkboxes and custom text, and the exam duration (Bearbeitungszeit).
 * It updates the corresponding fields in the {@link model.Exam} object and
 * provides a live preview of the generated instructions.
 */
public class HinweiseDialogController {

    @FXML
    private TextArea hinweiseAllgemeinArea;
    @FXML
    private CheckBox hinweisRechner;
    @FXML
    private CheckBox hinweisFormelsammlung;
    @FXML
    private CheckBox hinweisWoerterbuch;
    @FXML
    private CheckBox hinweisSkript;
    @FXML
    private CheckBox hinweisBuch;
    @FXML
    private TextField hinweisAndere;
    @FXML
    private Spinner<Integer> bearbeitungszeitSpinner;
    @FXML
    private Label hinweisePreviewLabel;
    @FXML
    private Button okButton;
    @FXML
    private Button cancelButton;

    private Stage dialogStage;
    private Exam exam;
    private boolean okClicked = false;

    /**
     * Initializes the controller after its root element has been completely processed.
     * This method sets up listeners for all input fields (text areas, checkboxes, spinner, text field)
     * to ensure the preview of the instructions is updated in real-time.
     */
    @FXML
    private void initialize() {
        hinweiseAllgemeinArea.textProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        hinweisRechner.selectedProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        hinweisFormelsammlung.selectedProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        hinweisWoerterbuch.selectedProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        hinweisSkript.selectedProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        hinweisBuch.selectedProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        hinweisAndere.textProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        bearbeitungszeitSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updatePreview());
    }

    /**
     * Sets the stage for this dialog.
     * @param dialogStage The {@link javafx.stage.Stage} object representing this dialog.
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Populates the dialog's UI elements with existing data from an {@link model.Exam} object.
     * This includes setting general instructions, exam duration, and parsing
     * the allowed aids string to correctly set the checkboxes and custom text field.
     * @param exam The {@link model.Exam} object containing the data to be displayed and edited.
     */
    public void setData(Exam exam) {
        this.exam = exam;

        hinweiseAllgemeinArea.setText(WordExporter.getStandardHinweise());

        bearbeitungszeitSpinner.getValueFactory().setValue(exam.getBearbeitungszeit() > 0 ? exam.getBearbeitungszeit() : 90);
        String hilfsmittel = exam.getHilfsmittel() != null ? exam.getHilfsmittel() : "";
        hinweisRechner.setSelected(hilfsmittel.contains("Taschenrechner"));
        hinweisFormelsammlung.setSelected(hilfsmittel.contains("Formelsammlung"));
        hinweisWoerterbuch.setSelected(hilfsmittel.contains("Wörterbuch"));
        hinweisSkript.setSelected(hilfsmittel.contains("Vorlesungsskript"));
        hinweisBuch.setSelected(hilfsmittel.contains("Buch"));

        String[] parts = hilfsmittel.split(", ");
        StringBuilder otherAids = new StringBuilder();
        for(String part : parts) {
            if (!part.equals("Taschenrechner") && !part.equals("Formelsammlung") && !part.equals("Wörterbuch") && !part.equals("Vorlesungsskript") && !part.equals("Buch")) {
                if(otherAids.length() > 0) otherAids.append(", ");
                otherAids.append(part);
            }
        }
        hinweisAndere.setText(otherAids.toString());
        
        updatePreview();
    }

    /**
     * Dynamically generates and displays a preview of the exam instructions
     * in the {@code hinweisePreviewLabel}. It updates the exam duration
     * and lists all selected allowed aids (from checkboxes and the custom text field)
     * within the instruction text.
     */
    private void updatePreview() {
        String previewText = hinweiseAllgemeinArea.getText();

        int zeit = bearbeitungszeitSpinner.getValue();
        previewText = previewText.replaceAll("Die Bearbeitungszeit beträgt \\d+ Minuten", "Die Bearbeitungszeit beträgt " + zeit + " Minuten");

        List<String> selectedAids = new ArrayList<>();
        if (hinweisRechner.isSelected()) selectedAids.add("Taschenrechner");
        if (hinweisFormelsammlung.isSelected()) selectedAids.add("Formelsammlung");
        if (hinweisWoerterbuch.isSelected()) selectedAids.add("Wörterbuch");
        if (hinweisSkript.isSelected()) selectedAids.add("Vorlesungsskript");
        if (hinweisBuch.isSelected()) selectedAids.add("Buch");
        String andere = hinweisAndere.getText();
        if (andere != null && !andere.trim().isEmpty()) {
            selectedAids.add(andere.trim());
        }

        String hilfsmittelText;
        if (selectedAids.isEmpty()) {
            hilfsmittelText = "Keine.";
        } else {
            hilfsmittelText = String.join(", ", selectedAids) + ".";
        }
        
        previewText = previewText.replaceAll("Elektronische und nicht elektronische Hilfsmittel sind nicht zugelassen, mit Ausnahme eines Taschenrechners \\(kein Smartphone!\\)", "Zugelassene Hilfsmittel: " + hilfsmittelText);

        hinweisePreviewLabel.setText(previewText);
    }

    /**
     * Returns true if the user clicked the OK button, false otherwise.
     * @return {@code true} if OK was clicked, {@code false} otherwise.
     */
    public boolean isOkClicked() {
        return okClicked;
    }

    /**
     * Handles the action when the OK button is clicked.
     * It saves the currently displayed instructions, exam duration,
     * and selected aids back into the {@link model.Exam} object.
     * Sets {@code okClicked} to true and closes the dialog stage.
     */
    @FXML
    private void handleOk() {
        exam.setAllgemeineHinweise(hinweisePreviewLabel.getText());
        exam.setBearbeitungszeit(bearbeitungszeitSpinner.getValue());
        
        List<String> selectedAids = new ArrayList<>();
        if (hinweisRechner.isSelected()) selectedAids.add("Taschenrechner");
        if (hinweisFormelsammlung.isSelected()) selectedAids.add("Formelsammlung");
        if (hinweisWoerterbuch.isSelected()) selectedAids.add("Wörterbuch");
        if (hinweisSkript.isSelected()) selectedAids.add("Vorlesungsskript");
        if (hinweisBuch.isSelected()) selectedAids.add("Buch");
        String andere = hinweisAndere.getText();
        if (andere != null && !andere.trim().isEmpty()) {
            selectedAids.add(andere.trim());
        }
        exam.setHilfsmittel(String.join(", ", selectedAids));

        okClicked = true;
        dialogStage.close();
    }

    /**
     * Handles the action when the Cancel button is clicked.
     * Closes the dialog stage without saving any changes to the {@link model.Exam} object.
     */
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
}