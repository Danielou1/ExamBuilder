package controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Exam;
import model.Question;
import service.WordExporter;
import utils.LoadingIndicator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import utils.Rephraser;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

public class MainController {

    @FXML
    private Button newQuestionButton;

    @FXML
    private TextField examTitleField;
    @FXML
    private TextField moduleField;
    @FXML
    private TextField semesterField;
    @FXML
    private TextField fachbereichField;
    @FXML
    private TextField hochschuleField;
    @FXML
    private TextField hilfsmittelField;

    @FXML
    private TreeTableView<Question> questionsTable;
    @FXML
    private TreeTableColumn<Question, String> titleColumn;
    @FXML
    private TreeTableColumn<Question, String> typeColumn;
    @FXML
    private TreeTableColumn<Question, String> pointsColumn;

    @FXML
    private VBox editPane;
    @FXML
    private TextArea questionTitleField;
    @FXML
    private TextArea questionTextField;
    @FXML
    private TextField questionPointsField;
    @FXML
    private ComboBox<String> questionTypeField;
    @FXML
    private Spinner<Integer> answerLinesField;

    @FXML
    private MenuItem addQuestionMenuItem;
    @FXML
    private MenuItem addSubQuestionMenuItem;
    @FXML
    private MenuItem updateQuestionMenuItem;
    @FXML
    private MenuItem deleteQuestionMenuItem;

    @FXML
    private Label totalPointsLabel;

    @FXML
    private BorderPane mainPane;

    @FXML
    private ToggleButton themeToggleButton;

    private Exam exam;

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getTitle()));
        typeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getType()));
        pointsColumn.setCellValueFactory(param -> new SimpleStringProperty(Integer.toString(param.getValue().getValue().getPoints())));

        TreeItem<Question> root = new TreeItem<>(new Question("Examen", "", 0, "", 0));
        questionsTable.setRoot(root);
        questionsTable.setShowRoot(false);

        exam = new Exam("", "", "", "", "", "", "");

        questionTypeField.getItems().addAll("Offene Frage", "MCQ");

        questionTypeField.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("MCQ".equals(newVal)) {
                questionTextField.setPromptText("Geben Sie die Frage ein, gefolgt von den Antwortmöglichkeiten im Format:\nA) Antwort 1\nB) Antwort 2\nC) Antwort 3");
                answerLinesField.setDisable(true);
                answerLinesField.getValueFactory().setValue(0);
            } else {
                questionTextField.setPromptText("Aufgabentext");
                answerLinesField.setDisable(false);
            }
        });

        questionsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                populateQuestionDetails(newValue.getValue());
                setEditMode(true);
            } else {
                clearQuestionFields();
                setEditMode(false);
            }
        });

        if (mainPane != null) {
            mainPane.getStylesheets().add(getClass().getResource("/styles/light-theme.css").toExternalForm());
        }

        if (themeToggleButton != null) {
            themeToggleButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.MOON_ALT));
            themeToggleButton.setOnAction(event -> {
                if (themeToggleButton.isSelected()) {
                    mainPane.getStylesheets().remove(getClass().getResource("/styles/light-theme.css").toExternalForm());
                    mainPane.getStylesheets().add(getClass().getResource("/styles/dark-theme.css").toExternalForm());
                    themeToggleButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.SUN_ALT));
                } else {
                    mainPane.getStylesheets().remove(getClass().getResource("/styles/dark-theme.css").toExternalForm());
                    mainPane.getStylesheets().add(getClass().getResource("/styles/light-theme.css").toExternalForm());
                    themeToggleButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.MOON_ALT));
                }
            });
        }

        setEditMode(false);
        setTooltips();
        setIcons();
    }

    private void setIcons() {
        newQuestionButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PLUS));
        addQuestionMenuItem.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PLUS_CIRCLE));
        addSubQuestionMenuItem.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PLUS_SQUARE));
        updateQuestionMenuItem.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.EDIT));
        deleteQuestionMenuItem.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
    }

    private void setTooltips() {
        Tooltip.install(newQuestionButton, new Tooltip("Neue Frage erstellen"));
        Tooltip.install(addQuestionMenuItem.getGraphic(), new Tooltip("Frage hinzufügen"));
        Tooltip.install(addSubQuestionMenuItem.getGraphic(), new Tooltip("Sub-Frage hinzufügen"));
        Tooltip.install(updateQuestionMenuItem.getGraphic(), new Tooltip("Frage aktualisieren"));
        Tooltip.install(deleteQuestionMenuItem.getGraphic(), new Tooltip("Frage löschen"));
    }


    private void setEditMode(boolean isEditing) {
        editPane.setDisable(!isEditing);
        addQuestionMenuItem.setDisable(isEditing);
        addSubQuestionMenuItem.setDisable(!isEditing);
        updateQuestionMenuItem.setDisable(!isEditing);
        deleteQuestionMenuItem.setDisable(!isEditing);
    }

    @FXML
    private void newQuestion() {
        questionsTable.getSelectionModel().clearSelection();
        clearQuestionFields();
        editPane.setDisable(false);
        addQuestionMenuItem.setDisable(false);
        addSubQuestionMenuItem.setDisable(true);
        updateQuestionMenuItem.setDisable(true);
        deleteQuestionMenuItem.setDisable(true);
    }

    private void populateQuestionDetails(Question question) {
        questionTitleField.setText(question.getTitle());
        questionTextField.setText(question.getText());
        questionPointsField.setText(String.valueOf(question.getPoints()));
        questionTypeField.setValue(question.getType());
        answerLinesField.getValueFactory().setValue(question.getAnswerLines());
    }

    private void refreshTreeTableView() {
        TreeItem<Question> root = new TreeItem<>(new Question("Examen", "", 0, "", 0));
        for (Question q : exam.getQuestions()) {
            TreeItem<Question> questionItem = new TreeItem<>(q);
            populateSubQuestions(questionItem, q);
            root.getChildren().add(questionItem);
        }
        questionsTable.setRoot(root);
        updateTotalPoints();
    }

    private void populateSubQuestions(TreeItem<Question> parentItem, Question parentQuestion) {
        if (parentQuestion.getSubQuestions() != null) {
            for (Question subQ : parentQuestion.getSubQuestions()) {
                TreeItem<Question> subItem = new TreeItem<>(subQ);
                parentItem.getChildren().add(subItem);
                populateSubQuestions(subItem, subQ);
            }
        }
    }

    @FXML
    private void addQuestion() {
        if (isQuestionInputInvalid()) return;
        Question newQuestion = createQuestionFromInput();
        exam.addQuestion(newQuestion);
        refreshTreeTableView();
        clearQuestionFields();
        setEditMode(false);
    }

    @FXML
    private void addSubQuestion() {
        if (isSubQuestionInputInvalid()) return;
        TreeItem<Question> selectedItem = questionsTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Question parentQuestion = selectedItem.getValue();
            Question subQuestion = createQuestionFromInput();
            parentQuestion.addSubQuestion(subQuestion);
            refreshTreeTableView();
            clearQuestionFields();
            setEditMode(false);
        } else {
            System.out.println("Please select a parent question first.");
        }
    }

    @FXML
    private void updateQuestion() {
        if (isQuestionInputInvalid()) return;
        TreeItem<Question> selectedItem = questionsTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Question questionToUpdate = selectedItem.getValue();
            questionToUpdate.setTitle(questionTitleField.getText());
            questionToUpdate.setText(questionTextField.getText());
            if (questionToUpdate.getSubQuestions() == null || questionToUpdate.getSubQuestions().isEmpty()) {
                questionToUpdate.setPoints(Integer.parseInt(questionPointsField.getText()));
            }
            questionToUpdate.setType(questionTypeField.getValue());
            if (!answerLinesField.isDisable()) {
                questionToUpdate.setAnswerLines(answerLinesField.getValue());
            }
            refreshTreeTableView();
            clearQuestionFields();
            setEditMode(false);
        } else {
            System.out.println("Please select a question to update.");
        }
    }

    @FXML
    private void deleteQuestion() {
        TreeItem<Question> selectedItem = questionsTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Frage löschen");
            alert.setHeaderText("Sind Sie sicher, dass Sie diese Frage löschen möchten?");
            alert.setContentText("Diese Aktion kann nicht rückgängig gemacht werden.");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    TreeItem<Question> parent = selectedItem.getParent();
                    if (parent != null && parent != questionsTable.getRoot()) {
                        parent.getValue().getSubQuestions().remove(selectedItem.getValue());
                    } else {
                        exam.getQuestions().remove(selectedItem.getValue());
                    }
                    refreshTreeTableView();
                    setEditMode(false);
                }
            });
        } else {
            System.out.println("Please select a question to delete.");
        }
    }

    private boolean isQuestionInputInvalid() {
        if (questionPointsField.getText().isEmpty()) {
            System.out.println("Points are required fields for a main question.");
            return true;
        }
        try {
            Integer.parseInt(questionPointsField.getText());
        } catch (NumberFormatException e) {
            System.out.println("Points must be a valid number.");
            return true;
        }
        return false;
    }

    private boolean isSubQuestionInputInvalid() {
        if (questionPointsField.getText().isEmpty()) {
            System.out.println("Points is required for a sub-question.");
            return true;
        }
        try {
            Integer.parseInt(questionPointsField.getText());
        } catch (NumberFormatException e) {
            System.out.println("Points must be a valid number.");
            return true;
        }
        return false;
    }

    private Question createQuestionFromInput() {
        String title = questionTitleField.getText();
        String text = questionTextField.getText();
        int points = 0;
        if (!questionPointsField.getText().isEmpty()) {
            points = Integer.parseInt(questionPointsField.getText());
        }
        String type = questionTypeField.getValue();
        int answerLines = 0;
        if (!answerLinesField.isDisable()) {
            answerLines = answerLinesField.getValue();
        }
        return new Question(title, text, points, type, answerLines);
    }

    @FXML
    private void exportToWord() {
        updateExamMetadata();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Exam as Word Document");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word Documents", "*.docx"));
        fileChooser.setInitialFileName(exam.getTitle() + ".docx");
        Stage stage = (Stage) examTitleField.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            Task<Void> exportTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    WordExporter.export(exam, file.getAbsolutePath(), hilfsmittelField.getText());
                    return null;
                }
            };

            exportTask.setOnSucceeded(e -> LoadingIndicator.hide());
            exportTask.setOnFailed(e -> {
                LoadingIndicator.hide();
                // Handle exceptions from the task
                exportTask.getException().printStackTrace();
            });

            new Thread(exportTask).start();
            LoadingIndicator.show();
        }
    }

    @FXML
    private void saveExamToJson() {
        updateExamMetadata();
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Exam as JSON");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            fileChooser.setInitialFileName(exam.getTitle() + ".json");
            Stage stage = (Stage) examTitleField.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                mapper.writeValue(file, exam);
                System.out.println("Exam saved to JSON: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void exportVariedVersion() {
        updateExamMetadata();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Varied Exam");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Word Documents", "*.docx"),
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );
        fileChooser.setInitialFileName(exam.getTitle() + "_varied");
        Stage stage = (Stage) examTitleField.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            LoadingIndicator.show();

            Task<Exam> rephraseTask = new Task<>() {
                @Override
                protected Exam call() throws Exception {
                    Exam variedExam = new Exam(exam);
                    List<Question> rephrasedQuestions = new ArrayList<>();
                    for (Question originalQuestion : variedExam.getQuestions()) {
                        rephrasedQuestions.add(rephraseQuestionRecursive(originalQuestion));
                    }
                    Collections.shuffle(rephrasedQuestions);
                    variedExam.setQuestions(rephrasedQuestions);
                    return variedExam;
                }
            };

            rephraseTask.setOnSucceeded(e -> {
                Exam variedExam = rephraseTask.getValue();
                Task<Void> exportTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        String fileName = file.getName();
                        if (fileName.endsWith(".docx")) {
                            WordExporter.export(variedExam, file.getAbsolutePath(), hilfsmittelField.getText());
                        } else if (fileName.endsWith(".json")) {
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                                mapper.writeValue(file, variedExam);
                                System.out.println("Varied Exam saved to JSON: " + file.getAbsolutePath());
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        } else {
                            System.out.println("Unsupported file type selected.");
                        }
                        return null;
                    }
                };

                exportTask.setOnSucceeded(event -> LoadingIndicator.hide());
                exportTask.setOnFailed(event -> {
                    LoadingIndicator.hide();
                    exportTask.getException().printStackTrace();
                });

                new Thread(exportTask).start();
            });

            rephraseTask.setOnFailed(e -> {
                LoadingIndicator.hide();
                rephraseTask.getException().printStackTrace();
            });

            new Thread(rephraseTask).start();
        }
    }

    @FXML
    private void exportToPdf() {
        updateExamMetadata();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Exam as PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName(exam.getTitle() + ".pdf");
        Stage stage = (Stage) examTitleField.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            service.PdfExporter.export(exam, file.getAbsolutePath(), hilfsmittelField.getText());
        }
    }

    @FXML
    private void importExamFromJson() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Exam JSON File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            Stage stage = (Stage) examTitleField.getScene().getWindow();
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                exam = mapper.readValue(file, Exam.class);
                updateUIFromExam();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateUIFromExam() {
        examTitleField.setText(exam.getTitle());
        moduleField.setText(exam.getModule());
        semesterField.setText(exam.getSemester());
        fachbereichField.setText(exam.getFachbereich());
        hochschuleField.setText(exam.getHochschule());
        if (exam.getHilfsmittel() != null) {
            hilfsmittelField.setText(exam.getHilfsmittel());
        }
        refreshTreeTableView();
    }

    private void updateExamMetadata() {
        exam.setTitle(examTitleField.getText());
        exam.setModule(moduleField.getText());
        exam.setSemester(semesterField.getText());
        exam.setFachbereich(fachbereichField.getText());
        exam.setHochschule(hochschuleField.getText());
        exam.setHilfsmittel(hilfsmittelField.getText());
    }

    private void updateTotalPoints() {
        totalPointsLabel.setText("Gesamtpunkte: " + exam.getTotalPoints());
    }

    private void clearQuestionFields() {
        questionTitleField.clear();
        questionTextField.clear();
        questionPointsField.clear();
        questionTypeField.setValue(null);
        answerLinesField.getValueFactory().setValue(0);
    }

    private Question rephraseQuestionRecursive(Question originalQuestion) {
        String rephrasedTitle = Rephraser.rephrase(originalQuestion.getTitle());
        String rephrasedText = Rephraser.rephrase(originalQuestion.getText());
        Question rephrasedQuestion = new Question(
                rephrasedTitle,
                rephrasedText,
                originalQuestion.getPoints(),
                originalQuestion.getType(),
                originalQuestion.getAnswerLines()
        );

        if (originalQuestion.getSubQuestions() != null && !originalQuestion.getSubQuestions().isEmpty()) {
            List<Question> rephrasedSubQuestions = new ArrayList<>();
            for (Question originalSubQuestion : originalQuestion.getSubQuestions()) {
                rephrasedSubQuestions.add(rephraseQuestionRecursive(originalSubQuestion));
            }
            Collections.shuffle(rephrasedSubQuestions);
            rephrasedQuestion.setSubQuestions(rephrasedSubQuestions);
        }
        return rephrasedQuestion;
    }
}