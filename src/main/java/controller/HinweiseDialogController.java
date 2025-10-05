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

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

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

    public boolean isOkClicked() {
        return okClicked;
    }

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

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
}