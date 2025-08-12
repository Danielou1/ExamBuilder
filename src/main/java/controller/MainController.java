package controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
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

public class MainController {

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

        questionTypeField.getItems().addAll("Offene Frage", "QCM");

        questionsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                populateQuestionDetails(newValue.getValue());
                setEditMode(true);
            } else {
                clearQuestionFields();
                setEditMode(false);
            }
        });

        setEditMode(false);
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
            questionToUpdate.setAnswerLines(answerLinesField.getValue());
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
            TreeItem<Question> parent = selectedItem.getParent();
            if (parent != null && parent != questionsTable.getRoot()) {
                parent.getValue().getSubQuestions().remove(selectedItem.getValue());
            } else {
                exam.getQuestions().remove(selectedItem.getValue());
            }
            refreshTreeTableView();
            setEditMode(false);
        } else {
            System.out.println("Please select a question to delete.");
        }
    }

    private boolean isQuestionInputInvalid() {
        if (questionTitleField.getText().isEmpty() || questionPointsField.getText().isEmpty()) {
            System.out.println("Title and Points are required fields for a main question.");
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
        int points = Integer.parseInt(questionPointsField.getText());
        String type = questionTypeField.getValue();
        int answerLines = answerLinesField.getValue();
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

        // 1. Create a copy of the exam to avoid modifying the original.
        Exam variedExam = new Exam(exam);

        // 2. Create a new list of rephrased questions.
        List<Question> rephrasedQuestions = new ArrayList<>();
        for (Question originalQuestion : variedExam.getQuestions()) {
            rephrasedQuestions.add(rephraseQuestionRecursive(originalQuestion));
        }

        // 3. Shuffle the new list of rephrased questions.
        Collections.shuffle(rephrasedQuestions);

        // 4. Set the rephrased and shuffled list on our new exam.
        variedExam.setQuestions(rephrasedQuestions);

        // 5. Proceed with exporting the varied exam.
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Varied Exam");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Word Documents", "*.docx"),
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );
        fileChooser.setInitialFileName(variedExam.getTitle() + "_varied");
        Stage stage = (Stage) examTitleField.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
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
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("Unsupported file type selected.");
                    }
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
