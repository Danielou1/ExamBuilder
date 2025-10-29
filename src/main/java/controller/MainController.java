package controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.controlsfx.control.textfield.TextFields;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Exam;
import model.Question;
import service.WordExporter;
import utils.LoadingIndicator;
import utils.Rephraser;

public class MainController {

    @FXML
    private Button newQuestionButton;
    @FXML
    private Button addImageButton;
    @FXML
    private Button hinweiseButton;

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
    private TreeTableView<Question> questionsTable;
    @FXML
    private TreeTableColumn<Question, Boolean> selectedColumn;
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
    private HTMLEditor questionTextField;
    @FXML
    private TextArea musterloesungField;
    @FXML
    private TextField questionPointsField;
    @FXML
    private ComboBox<String> questionTypeField;
    @FXML
    private Spinner<Integer> answerLinesField;
    @FXML
    private ImageView questionImageView;
    @FXML
    private Button addSolutionImageButton;
    @FXML
    private ImageView musterloesungImageView;

    @FXML
    private MenuItem addQuestionMenuItem;
    @FXML
    private MenuItem addSubQuestionMenuItem;
    @FXML
    private MenuItem editQuestionMenuItem;
    @FXML
    private MenuItem updateQuestionMenuItem;
    @FXML
    private MenuItem deleteQuestionMenuItem;
    @FXML
    private MenuButton actionsMenuButton;

    @FXML
    private Label totalPointsLabel;

    @FXML
    private BorderPane mainPane;

    @FXML
    private ToggleButton themeToggleButton;
    @FXML
    private ToggleButton selectionModeButton;

    private Exam exam;
    private ContextMenu tableContextMenu;
    private List<String> germanUniversities;
    private String newQuestionImageBase64 = null;
    private String newQuestionSolutionImageBase64 = null;
    private TreeItem<Question> parentForSubQuestion = null;
    private Stage hinweiseDialogStage;
    private HinweiseDialogController hinweiseDialogController;
    private Question originalQuestionState;
    private boolean isRevertingSelection = false;
    private boolean isPopulatingUI = false;
    private ChangeListener<String> questionTypeChangeListener;

    // ButtonTypes for unsaved changes dialog
    private final ButtonType saveButton = new ButtonType("Änderungen speichern");
    private final ButtonType continueButton = new ButtonType("Ohne Speichern fortfahren");
    private final ButtonType cancelButton = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

    @FXML
    public void initialize() {
        questionsTable.setEditable(true);

        loadUniversities();
        TextFields.bindAutoCompletion(hochschuleField, germanUniversities);

        selectedColumn.setEditable(false);
        selectedColumn.setCellValueFactory(param -> param.getValue().getValue().selectedProperty());
        selectedColumn.setCellFactory(CheckBoxTreeTableCell.forTreeTableColumn(selectedColumn));

        setupRowFactory();
        setupContextMenu();
        setupImageContextMenu();

        titleColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getTitle()));
        typeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getType()));
        pointsColumn.setCellValueFactory(param -> new SimpleStringProperty(Integer.toString(param.getValue().getValue().getPoints())));

        TreeItem<Question> root = new TreeItem<>(new Question("Examen", "", 0, "", 0));
        questionsTable.setRoot(root);
        questionsTable.setShowRoot(false);

        exam = new Exam("", "", "", "", "", "", "");

        questionTypeField.getItems().addAll("Offene Frage", "MCQ", "Lückentext", "Richtig/Falsch");

        questionTypeChangeListener = (obs, oldVal, newVal) -> {
            if (isPopulatingUI) {
                return; // Skip listener logic if UI is being populated
            }
            // Disable answer lines for types that don't need them
            if ("MCQ".equals(newVal) || "Lückentext".equals(newVal) || "Richtig/Falsch".equals(newVal)) {
                answerLinesField.setDisable(true);
                answerLinesField.getValueFactory().setValue(0);
            } else {
                answerLinesField.setDisable(false);
            }

            // Update UI prompts and placeholders based on the selected question type
            switch (newVal != null ? newVal : "") {
                case "MCQ":
                    questionTitleField.setPromptText("Geben Sie hier die vollständige Frage für den MCQ ein.");
                    questionTextField.setHtmlText("<p style=\"color:grey;\"><i>Geben Sie hier die Antwortmöglichkeiten als Liste ein (z.B. A, B, C).</i></p>");
                    musterloesungField.setPromptText("Korrekte Buchstaben trennen (z.B. A, C)");
                    break;
                case "Lückentext":
                    questionTitleField.setPromptText("Geben Sie hier den Titel der Aufgabe ein (optional).");
                    // Only set placeholder if the HTMLEditor is empty
                    if (questionTextField.getHtmlText().isEmpty() || questionTextField.getHtmlText().equals("<html dir=\"ltr\"><head></head><body contenteditable=\"true\"></body></html>")) {
                        questionTextField.setHtmlText("<p style=\"color:grey;\"><i>Schreiben Sie hier den Textkörper und verwenden Sie \'___\' für jede Lücke.</i></p>");
                    }
                    musterloesungField.setPromptText("Antworten mit Semikolon trennen (z.B. Antwort1; Antwort2)");
                    break;
                case "Richtig/Falsch":
                    questionTitleField.setPromptText("Geben Sie hier die Aussage ein, die bewertet werden soll.");
                    // Only clear if the HTMLEditor is empty or contains a placeholder
                    if (questionTextField.getHtmlText().isEmpty() || questionTextField.getHtmlText().contains("<i>Geben Sie hier den Aufgabentext")) {
                        questionTextField.setHtmlText("");
                    }
                    musterloesungField.setPromptText("Geben Sie 'Richtig' oder 'Falsch' als Lösung ein.");
                    break;
                case "Offene Frage":
                default:
                    questionTitleField.setPromptText("Geben Sie hier den Titel der Aufgabe ein (optional).");
                    questionTextField.setHtmlText("<p style=\"color:grey;\"><i>Geben Sie hier den Aufgabentext oder weitere Anweisungen ein.</i></p>");
                    musterloesungField.setPromptText("Geben Sie hier die textuelle Musterlösung ein.");
                    break;
            }
        };
        questionTypeField.valueProperty().addListener(questionTypeChangeListener);

        questionsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (isRevertingSelection) {
                return; // Avoid re-entrancy
            }

            if (newValue != null) {
                // If there's an old value (meaning we're switching from one question to another)
                // and the edit pane is active (meaning we were editing something)
                if (oldValue != null && !editPane.isDisable()) {
                    // Check for unsaved changes on the *oldValue* (the question we were just editing)
                    if (areChangesMade()) {
                        Optional<ButtonType> result = showUnsavedChangesConfirmation();

                        if (result.isPresent()) {
                            if (result.get() == saveButton) {
                                if (!updateQuestionAndReturnSuccess()) {
                                    // Save failed, revert selection and stay on oldValue
                                    isRevertingSelection = true;
                                    questionsTable.getSelectionModel().select(oldValue);
                                    isRevertingSelection = false;
                                    return;
                                }
                                // Save successful, proceed to newValue
                            } else if (result.get() == continueButton) {
                                // Proceed to newValue, changes to oldValue are discarded
                            } else if (result.get() == cancelButton) {
                                // User canceled, revert selection and stay on oldValue
                                isRevertingSelection = true;
                                questionsTable.getSelectionModel().select(oldValue);
                                isRevertingSelection = false;
                                return;
                            }
                        } else {
                            // Dialog closed unexpectedly, revert selection and stay on oldValue
                            isRevertingSelection = true;
                            questionsTable.getSelectionModel().select(oldValue);
                            isRevertingSelection = false;
                            return;
                        }
                    }
                }
                // If we reach here, it means either no changes were made, or changes were saved/discarded.
                // Now, populate the UI with the details of the *newValue*.
                parentForSubQuestion = null; // A new selection always resets the sub-question context
                populateQuestionDetails(newValue.getValue());
            } else {
                // newValue is null, clear fields if not creating a sub-question
                if (parentForSubQuestion == null) {
                    clearQuestionFields();
                }
            }
            updateActionsState();
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
        updateActionsState();

        questionTextField.focusedProperty().addListener((obs, oldVal, isFocused) -> {
            if (!isFocused) { // The editor lost focus
                TreeItem<Question> selectedItem = questionsTable.getSelectionModel().getSelectedItem();
                if (selectedItem != null && !editPane.isDisable()) {
                    Question questionToUpdate = selectedItem.getValue();
                    if (questionToUpdate.getText() == null || !questionToUpdate.getText().equals(questionTextField.getHtmlText())) {
                        questionToUpdate.setText(questionTextField.getHtmlText());
                    }
                }
            }
        });
    }

    private void setupImageContextMenu() {
        ContextMenu imageContextMenu = new ContextMenu();
        MenuItem removeItem = new MenuItem("Bild entfernen");
        removeItem.setOnAction(event -> removeImage());
        imageContextMenu.getItems().add(removeItem);

        questionImageView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY && questionImageView.getImage() != null) {
                imageContextMenu.show(questionImageView, event.getScreenX(), event.getScreenY());
            }
        });

        ContextMenu solutionImageContextMenu = new ContextMenu();
        MenuItem removeSolutionItem = new MenuItem("Lösungsbild entfernen");
        removeSolutionItem.setOnAction(event -> removeSolutionImage());
        solutionImageContextMenu.getItems().add(removeSolutionItem);

        musterloesungImageView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY && musterloesungImageView.getImage() != null) {
                solutionImageContextMenu.show(musterloesungImageView, event.getScreenX(), event.getScreenY());
            }
        });
    }

    private void loadUniversities() {
        germanUniversities = Arrays.asList(
            "RWTH Aachen",
            "Universität Augsburg",
            "Universität Bamberg",
            "Universität Bayreuth",
            "Freie Universität Berlin",
            "Humboldt-Universität zu Berlin",
            "Technische Universität Berlin",
            "Universität der Künste Berlin",
            "Universität Bielefeld",
            "Ruhr-Universität Bochum",
            "Universität Bonn",
            "Technische Universität Braunschweig",
            "Universität Bremen",
            "Technische Universität Chemnitz",
            "Technische Universität Clausthal",
            "Brandenburgische Technische Universität Cottbus-Senftenberg",
            "Technische Universität Darmstadt",
            "Technische Universität Dortmund",
            "Technische Universität Dresden",
            "Universität Duisburg-Essen",
            "Heinrich-Heine-Universität Düsseldorf",
            "Katholische Universität Eichstätt-Ingolstadt",
            "Universität Erfurt",
            "Friedrich-Alexander-Universität Erlangen-Nürnberg",
            "Goethe-Universität Frankfurt am Main",
            "Europa-Universität Viadrina Frankfurt (Oder)",
            "Technische Universität Bergakademie Freiberg",
            "Albert-Ludwigs-Universität Freiburg",
            "Justus-Liebig-Universität Gießen",
            "Georg-August-Universität Göttingen",
            "Universität Greifswald",
            "FernUniversität in Hagen",
            "Martin-Luther-Universität Halle-Wittenberg",
            "Universität Hamburg",
            "Technische Universität Hamburg",
            "Helmut-Schmidt-Universität/Universität der Bundeswehr Hamburg",
            "Leibniz Universität Hannover",
            "Ruprecht-Karls-Universität Heidelberg",
            "Stiftung Universität Hildesheim",
            "Technische Universität Ilmenau",
            "Friedrich-Schiller-Universität Jena",
            "Technische Universität Kaiserslautern",
            "Karlsruher Institut für Technologie (KIT)",
            "Universität Kassel",
            "Christian-Albrechts-Universität zu Kiel",
            "Universität zu Köln",
            "Universität Konstanz",
            "Universität Leipzig",
            "Universität zu Lübeck",
            "Otto-von-Guericke-Universität Magdeburg",
            "Johannes Gutenberg-Universität Mainz",
            "Universität Mannheim",
            "Philipps-Universität Marburg",
            "Ludwigs-Maximilians-Universität München (LMU)",
            "Technische Universität München (TUM)",
            "Universität der Bundeswehr München",
            "Westfälische Wilhelms-Universität Münster",
            "Carl von Ossietzky Universität Oldenburg",
            "Universität Osnabrück",
            "Universität Paderborn",
            "Universität Passau",
            "Universität Potsdam",
            "Universität Regensburg",
            "Universität Rostock",
            "Universität des Saarlandes",
            "Universität Siegen",
            "Universität Hohenheim",
            "Universität Stuttgart",
            "Universität Trier",
            "Eberhard Karls Universität Tübingen",
            "Universität Ulm",
            "Bauhaus-Universität Weimar",
            "Julius-Maximilians-Universität Würzburg",
            "Bergische Universität Wuppertal",
            "Technische Hochschule Mittelhessen (THM)"
        );
    }

    private void setupRowFactory() {
        questionsTable.setRowFactory(tv -> {
            TreeTableRow<Question> row = new TreeTableRow<>() {
                @Override
                protected void updateItem(Question item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().remove("deselected-row");
                    if (item != null && !empty) {
                        item.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                            if (isNowSelected) {
                                getStyleClass().remove("deselected-row");
                            } else {
                                getStyleClass().add("deselected-row");
                            }
                        });
                        if (!item.isSelected()) {
                            getStyleClass().add("deselected-row");
                        }
                    }
                }
            };
            return row;
        });
    }

    private void setupContextMenu() {
        tableContextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Frage bearbeiten");
        editItem.setOnAction(e -> editQuestion());
        MenuItem saveItem = new MenuItem("Änderungen speichern");
        saveItem.setOnAction(e -> updateQuestionAndReturnSuccess());
        MenuItem deleteItem = new MenuItem("Frage löschen");
        deleteItem.setOnAction(e -> deleteQuestion());
        MenuItem addSubItem = new MenuItem("Sub-Frage hinzufügen");
        addSubItem.setOnAction(e -> addSubQuestion());

        tableContextMenu.getItems().addAll(editItem, saveItem, new SeparatorMenuItem(), addSubItem, deleteItem);

        tableContextMenu.setOnShowing(e -> {
            boolean noSelection = questionsTable.getSelectionModel().getSelectedItem() == null;
            boolean isEditing = !editPane.isDisable();
            editItem.setDisable(noSelection || isEditing);
            saveItem.setDisable(noSelection || !isEditing);
            deleteItem.setDisable(noSelection);
            addSubItem.setDisable(noSelection || isEditing);
        });

        questionsTable.setContextMenu(tableContextMenu);
    }

    private Stage primaryStage;

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        createHinweiseDialog();
    }

    private void createHinweiseDialog() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/HinweiseDialog.fxml"));
            VBox page = loader.load();

            hinweiseDialogStage = new Stage();
            hinweiseDialogStage.setTitle("Hinweise, Hilfsmittel & Zeit bearbeiten");
            hinweiseDialogStage.initModality(Modality.WINDOW_MODAL);
            hinweiseDialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            hinweiseDialogStage.setScene(scene);

            hinweiseDialogController = loader.getController();
            hinweiseDialogController.setDialogStage(hinweiseDialogStage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openHinweiseDialog() {
        if (hinweiseDialogStage != null) {
            hinweiseDialogController.setData(this.exam);
            hinweiseDialogStage.showAndWait();
        } else {
            System.err.println("Hinweise Dialog could not be created.");
        }
    }

    @FXML
    private void toggleSelectionMode() {
        boolean selectionModeActive = selectionModeButton.isSelected();
        selectedColumn.setEditable(selectionModeActive);

        newQuestionButton.setDisable(selectionModeActive);
        addImageButton.setDisable(selectionModeActive);
        actionsMenuButton.setDisable(selectionModeActive);
        editPane.setDisable(selectionModeActive);
        hinweiseButton.setDisable(selectionModeActive);
        
        if (selectionModeActive) {
            questionsTable.setContextMenu(null);
            selectionModeButton.setText("Bearbeitungsmodus");
            questionsTable.getSelectionModel().clearSelection();
        } else {
            questionsTable.setContextMenu(tableContextMenu);
            selectionModeButton.setText("Auswahl für Export");
            updateActionsState();
        }
    }

    private void setIcons() {
        newQuestionButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PLUS));
        addImageButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.FILE_IMAGE_ALT));
        addQuestionMenuItem.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PLUS_CIRCLE));
        addSubQuestionMenuItem.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PLUS_SQUARE));
        editQuestionMenuItem.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PENCIL_SQUARE_ALT));
        updateQuestionMenuItem.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.SAVE));
        deleteQuestionMenuItem.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
    }

    private void setTooltips() {
        Tooltip.install(newQuestionButton, new Tooltip("Neue Frage erstellen"));
        Tooltip.install(hinweiseButton, new Tooltip("Hinweise, Hilfsmittel und Bearbeitungszeit für die Prüfung festlegen"));
        Tooltip.install(addImageButton, new Tooltip("Ein Bild zu dieser Frage hinzufügen"));
        Tooltip.install(addQuestionMenuItem.getGraphic(), new Tooltip("Frage hinzufügen"));
        Tooltip.install(addSubQuestionMenuItem.getGraphic(), new Tooltip("Sub-Frage hinzufügen"));
        Tooltip.install(editQuestionMenuItem.getGraphic(), new Tooltip("Frage bearbeiten"));
        Tooltip.install(updateQuestionMenuItem.getGraphic(), new Tooltip("Änderungen speichern"));
        Tooltip.install(deleteQuestionMenuItem.getGraphic(), new Tooltip("Frage löschen"));
    }

    private void setEditMode(boolean isEditing) {
        editPane.setDisable(!isEditing);
        updateActionsState();
    }

    private void updateActionsState() {
        boolean selectionExists = questionsTable.getSelectionModel().getSelectedItem() != null;
        boolean isEditing = !editPane.isDisable();

        actionsMenuButton.setDisable(!(selectionExists || isEditing));
        addImageButton.setDisable(!isEditing);

        boolean isUpdating = isEditing && selectionExists;
        
        if (parentForSubQuestion != null && isEditing) {
            addQuestionMenuItem.setText("Sub-Frage speichern");
        } else {
            addQuestionMenuItem.setText("Frage hinzufügen");
        }

        addQuestionMenuItem.setDisable(isUpdating);
        updateQuestionMenuItem.setDisable(!isUpdating);

        editQuestionMenuItem.setDisable(!selectionExists || isEditing);
        deleteQuestionMenuItem.setDisable(!selectionExists);
        addSubQuestionMenuItem.setDisable(!selectionExists || isEditing);
    }

    @FXML
    private void newQuestion() {
        if (areChangesMade()) { // Check for unsaved changes on the *currently edited* question
            Optional<ButtonType> result = showUnsavedChangesConfirmation();
            if (result.isPresent() && result.get() == cancelButton) {
                return; // User canceled, do not proceed with new question
            }
            // If user chose save (and it succeeded) or continue, proceed.
        }
        parentForSubQuestion = null;
        questionsTable.getSelectionModel().clearSelection();
        setEditMode(true);
        clearQuestionFields(); // Clear fields after handling unsaved changes
        originalQuestionState = null; // No question loaded, so no original state
    }

    @FXML
    private void editQuestion() {
        TreeItem<Question> selectedItem = questionsTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            parentForSubQuestion = null; // Editing a question is not creating a sub-question
            populateQuestionDetails(selectedItem.getValue());
            setEditMode(true);
        }
    }

    private void populateQuestionDetails(Question question) {
        // Temporarily remove listener to prevent unwanted side effects during UI population
        if (questionTypeChangeListener != null) {
            questionTypeField.valueProperty().removeListener(questionTypeChangeListener);
        }

        isPopulatingUI = true; // Set flag (still good practice)
        this.originalQuestionState = new Question(question); // Store a deep copy for change detection
        questionTitleField.setText(question.getTitle());
        questionTextField.setHtmlText(question.getText());
        musterloesungField.setText(question.getMusterloesung());
        questionPointsField.setText(String.valueOf(question.getPoints()));
        questionTypeField.setValue(question.getType());
        answerLinesField.getValueFactory().setValue(question.getAnswerLines());

        if (question.getImageBase64() != null && !question.getImageBase64().isEmpty()) {
            byte[] imageBytes = Base64.getDecoder().decode(question.getImageBase64());
            questionImageView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
        } else {
            questionImageView.setImage(null);
        }

        if (question.getMusterloesungImageBase64() != null && !question.getMusterloesungImageBase64().isEmpty()) {
            byte[] imageBytes = Base64.getDecoder().decode(question.getMusterloesungImageBase64());
            musterloesungImageView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
        } else {
            musterloesungImageView.setImage(null);
        }
        isPopulatingUI = false; // Clear flag

        // Re-add listener
        if (questionTypeChangeListener != null) {
            questionTypeField.valueProperty().addListener(questionTypeChangeListener);
        }
    }

    private void refreshTreeTableView() {
        TreeItem<Question> selectedItem = questionsTable.getSelectionModel().getSelectedItem();
        Question selectedQuestion = selectedItem != null ? selectedItem.getValue() : null;

        TreeItem<Question> root = new TreeItem<>(new Question("Examen", "", 0, "", 0));
        for (Question q : exam.getQuestions()) {
            TreeItem<Question> questionItem = new TreeItem<>(q);
            populateSubQuestions(questionItem, q);
            root.getChildren().add(questionItem);
        }
        questionsTable.setRoot(root);

        if (selectedQuestion != null) {
            findAndSelectQuestion(questionsTable.getRoot(), selectedQuestion);
        }
        updateTotalPoints();
    }
    
    private boolean findAndSelectQuestion(TreeItem<Question> current, Question target) {
        if (current.getValue().equals(target)) {
            questionsTable.getSelectionModel().select(current);
            return true;
        }
        for (TreeItem<Question> child : current.getChildren()) {
            if (findAndSelectQuestion(child, target)) {
                return true;
            }
        }
        return false;
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
        if (questionPointsField.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Punktzahl fehlt");
            alert.setHeaderText("Die Punktzahl für diese Frage fehlt.");
            alert.setContentText("Möchten Sie mit 0 Punkten fortfahren oder die Punktzahl manuell eingeben?");

            ButtonType continueButton = new ButtonType("Fortfahren mit 0 Punkten");
            ButtonType modifyButton = new ButtonType("Manuell eingeben", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(continueButton, modifyButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == continueButton) {
                questionPointsField.setText("0");
            } else {
                return;
            }
        }

        if (isPointsInvalid()) {
            return;
        }

        Question newQuestion = createQuestionFromInput();

        if (parentForSubQuestion != null) {
            parentForSubQuestion.getValue().addSubQuestion(newQuestion);
            parentForSubQuestion = null; // Reset context after use
        } else {
            exam.addQuestion(newQuestion);
        }

        refreshTreeTableView();
        clearQuestionFields();
        setEditMode(false);
    }

    @FXML
    private void addSubQuestion() {
        TreeItem<Question> selectedItem = questionsTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            parentForSubQuestion = selectedItem;
            questionsTable.getSelectionModel().clearSelection();
            setEditMode(true);
            clearQuestionFields(); // Clear fields for new sub-question
        }
    }

    public boolean updateQuestionAndReturnSuccess() {
        if (questionPointsField.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Punktzahl fehlt");
            alert.setHeaderText("Die Punktzahl für diese Frage fehlt.");
            alert.setContentText("Möchten Sie mit 0 Punkten fortfahren oder die Punktzahl manuell eingeben?");

            ButtonType continueButton = new ButtonType("Fortfahren mit 0 Punkten");
            ButtonType modifyButton = new ButtonType("Manuell eingeben", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(continueButton, modifyButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == continueButton) {
                questionPointsField.setText("0");
            } else {
                return false; // User chose not to proceed with 0 points
            }
        }

        if (isPointsInvalid()) {
            return false; // Points are invalid, cannot save
        }

        TreeItem<Question> selectedItem = questionsTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            try {
                Question questionToUpdate = selectedItem.getValue();
                questionToUpdate.setTitle(questionTitleField.getText());
                
                String questionText = questionTextField.getHtmlText();
                if ("MCQ".equals(questionToUpdate.getType())) {
                    questionText = normalizeMcqHtml(questionText);
                }
                questionToUpdate.setText(questionText);
                questionToUpdate.setMusterloesung(musterloesungField.getText());
                if (questionToUpdate.getSubQuestions() == null || questionToUpdate.getSubQuestions().isEmpty()) {
                    questionToUpdate.setPoints(Integer.parseInt(questionPointsField.getText()));
                }
                questionToUpdate.setType(questionTypeField.getValue());
                if (!answerLinesField.isDisable()) {
                    questionToUpdate.setAnswerLines(answerLinesField.getValue());
                }
                // Save the solution image base64 from the temporary field to the question object
                if (newQuestionSolutionImageBase64 != null) {
                    questionToUpdate.setMusterloesungImageBase64(newQuestionSolutionImageBase64);
                }
                refreshTreeTableView();
                this.originalQuestionState = new Question(questionToUpdate); // Update original state after successful save
                populateQuestionDetails(questionToUpdate); // Re-populate with updated details
                setEditMode(false);
                return true; // Save successful
            } catch (Exception e) {
                // Log the exception or show an error to the user
                e.printStackTrace();
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Fehler beim Speichern");
                errorAlert.setHeaderText("Die Änderungen konnten nicht gespeichert werden.");
                errorAlert.setContentText("Ein unerwarteter Fehler ist aufgetreten: " + e.getMessage());
                errorAlert.showAndWait();
                return false; // Save failed
            }
        } else {
            System.out.println("Please select a question to update.");
            return false; // No question selected
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

    @FXML
    private void addImage() {
        boolean isEditing = !editPane.isDisable();
        TreeItem<Question> selectedItem = questionsTable.getSelectionModel().getSelectedItem();

        if (selectedItem == null && !isEditing) {
             System.out.println("Please select or create a question first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Bild auswählen");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif", "*.bmp")
        );
        File selectedFile = fileChooser.showOpenDialog(mainPane.getScene().getWindow());

        if (selectedFile != null) {
            try {
                byte[] fileContent = Files.readAllBytes(selectedFile.toPath());
                String base64String = Base64.getEncoder().encodeToString(fileContent);
                
                boolean isNewQuestionMode = isEditing && selectedItem == null;

                if (isNewQuestionMode) {
                    newQuestionImageBase64 = base64String;
                } else if (selectedItem != null) {
                    selectedItem.getValue().setImageBase64(base64String);
                }

                questionImageView.setImage(new Image(new ByteArrayInputStream(fileContent)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void removeImage() {
        boolean isEditing = !editPane.isDisable();
        TreeItem<Question> selectedItem = questionsTable.getSelectionModel().getSelectedItem();
        boolean isNewQuestionMode = isEditing && selectedItem == null;

        if (isNewQuestionMode) {
            newQuestionImageBase64 = null;
        } else if (selectedItem != null) {
            selectedItem.getValue().setImageBase64(null);
        }
        questionImageView.setImage(null);
    }

    @FXML
    private void addSolutionImage() {
        boolean isEditing = !editPane.isDisable();
        TreeItem<Question> selectedItem = questionsTable.getSelectionModel().getSelectedItem();

        if (selectedItem == null && !isEditing) {
             System.out.println("Please select or create a question first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lösungsbild auswählen");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif", "*.bmp")
        );
        File selectedFile = fileChooser.showOpenDialog(mainPane.getScene().getWindow());

        if (selectedFile != null) {
            try {
                byte[] fileContent = Files.readAllBytes(selectedFile.toPath());
                String base64String = Base64.getEncoder().encodeToString(fileContent);
                
                boolean isNewQuestionMode = isEditing && selectedItem == null;

                if (isNewQuestionMode) {
                    newQuestionSolutionImageBase64 = base64String;
                } else if (selectedItem != null) {
                    selectedItem.getValue().setMusterloesungImageBase64(base64String);
                }

                musterloesungImageView.setImage(new Image(new ByteArrayInputStream(fileContent)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void removeSolutionImage() {
        boolean isEditing = !editPane.isDisable();
        TreeItem<Question> selectedItem = questionsTable.getSelectionModel().getSelectedItem();
        boolean isNewQuestionMode = isEditing && selectedItem == null;

        if (isNewQuestionMode) {
            newQuestionSolutionImageBase64 = null;
        } else if (selectedItem != null) {
            selectedItem.getValue().setMusterloesungImageBase64(null);
        }
        musterloesungImageView.setImage(null);
    }

    private boolean isPointsInvalid() {
        try {
            Integer.parseInt(questionPointsField.getText());
            return false;
        } catch (NumberFormatException e) {
            System.out.println("Points must be a valid number.");
            return true;
        }
    }

    private Question createQuestionFromInput() {
        String title = questionTitleField.getText();
        String text = questionTextField.getHtmlText();
        String type = questionTypeField.getValue();
        if ("MCQ".equals(type)) {
            text = normalizeMcqHtml(text);
        }
        int points = 0;
        if (!questionPointsField.getText().isEmpty()) {
            points = Integer.parseInt(questionPointsField.getText());
        }
        int answerLines = 0;
        if (!answerLinesField.isDisable()) {
            answerLines = answerLinesField.getValue();
        }
        Question newQuestion = new Question(title, text, points, type, answerLines);
        newQuestion.setMusterloesung(musterloesungField.getText());
        
        if (newQuestionImageBase64 != null) {
            newQuestion.setImageBase64(newQuestionImageBase64);
        }
        if (newQuestionSolutionImageBase64 != null) {
            newQuestion.setMusterloesungImageBase64(newQuestionSolutionImageBase64);
        }

        return newQuestion;
    }

    private List<Question> getQuestionsForExport() {
        List<Question> selectedQuestions = filterSelected(exam.getQuestions());

        if (selectedQuestions.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Export-Optionen");
            alert.setHeaderText("Keine Fragen für den Export ausgewählt.");
            alert.setContentText("Möchten Sie alle Fragen exportieren?");

            ButtonType buttonTypeExportAll = new ButtonType("Alle exportieren");
            ButtonType buttonTypeCancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(buttonTypeExportAll, buttonTypeCancel);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == buttonTypeExportAll) {
                return exam.getQuestions();
            } else {
                return null; // Cancel export
            }
        } else {
            return selectedQuestions;
        }
    }

    private List<Question> filterSelected(List<Question> questions) {
        List<Question> selected = new ArrayList<>();
        for (Question q : questions) {
            if (q.isSelected()) {
                Question copy = new Question(q.getTitle(), q.getText(), q.getPoints(), q.getType(), q.getAnswerLines());
                copy.setMusterloesung(q.getMusterloesung());
                copy.setSelected(q.getSelected());
                copy.setId(q.getId());
                copy.setImageBase64(q.getImageBase64()); // Copy image data

                if (q.getSubQuestions() != null && !q.getSubQuestions().isEmpty()) {
                    copy.setSubQuestions(filterSelected(q.getSubQuestions()));
                }
                selected.add(copy);
            }
        }
        return selected;
    }

    @FXML
    private void exportToWord() {
        updateExamMetadata();
        List<Question> questionsToExport = getQuestionsForExport();
        if (questionsToExport == null) return;

        Exam examToExport = new Exam(exam);
        examToExport.setQuestions(questionsToExport);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Exam as Word Document");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word Documents", "*.docx"));
        fileChooser.setInitialFileName(exam.getTitle() + ".docx");
        Stage stage = (Stage) mainPane.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            Task<Void> exportTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    WordExporter.export(examToExport, file.getAbsolutePath());
                    return null;
                }
            };
            exportTask.setOnSucceeded(e -> LoadingIndicator.hide());
            exportTask.setOnFailed(e -> {
                LoadingIndicator.hide();
                exportTask.getException().printStackTrace();
            });
            new Thread(exportTask).start();
            LoadingIndicator.show();
        }
    }

    @FXML
    private void exportAnswerKey() {
        updateExamMetadata();
        List<Question> questionsToExport = getQuestionsForExport();
        if (questionsToExport == null) return;

        Exam examToExport = new Exam(exam);
        examToExport.setQuestions(questionsToExport);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Answer Key as Word Document");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word Documents", "*.docx"));
        fileChooser.setInitialFileName(exam.getTitle() + "_Lösungen.docx");
        Stage stage = (Stage) mainPane.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            Task<Void> exportTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    WordExporter.exportWithSolutions(examToExport, file.getAbsolutePath());
                    return null;
                }
            };
            exportTask.setOnSucceeded(e -> LoadingIndicator.hide());
            exportTask.setOnFailed(e -> {
                LoadingIndicator.hide();
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
            Stage stage = (Stage) mainPane.getScene().getWindow();
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
        List<Question> questionsToExport = getQuestionsForExport();
        if (questionsToExport == null) return;

        Exam examToExport = new Exam(exam);
        examToExport.setQuestions(questionsToExport);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Varied Exam");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Word Documents", "*.docx"),
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );
        fileChooser.setInitialFileName(exam.getTitle() + "_varied");
        Stage stage = (Stage) mainPane.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            LoadingIndicator.show();

            Task<Exam> rephraseAndShuffleTask = new Task<>() {
                @Override
                protected Exam call() throws Exception {
                    Exam variedExam = new Exam(examToExport);
                    List<Question> processedQuestions = new ArrayList<>();
                    for (Question originalQuestion : variedExam.getQuestions()) {
                        processedQuestions.add(createVariedQuestionRecursive(originalQuestion));
                    }
                    variedExam.setQuestions(processedQuestions);
                    return variedExam;
                }
            };

            rephraseAndShuffleTask.setOnSucceeded(e -> {
                Exam variedExam = rephraseAndShuffleTask.getValue();
                Task<Void> exportTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        String fileName = file.getName();
                        if (fileName.endsWith(".docx")) {
                            WordExporter.export(variedExam, file.getAbsolutePath());
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

            rephraseAndShuffleTask.setOnFailed(e -> {
                LoadingIndicator.hide();
                rephraseAndShuffleTask.getException().printStackTrace();
            });

            new Thread(rephraseAndShuffleTask).start();
        }
    }

    @FXML
    private void importExamFromJson() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Exam JSON File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            Stage stage = (Stage) mainPane.getScene().getWindow();
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                exam = mapper.readValue(file, Exam.class);

                // Automatically convert plain-text MCQs to HTML
                if (exam.getQuestions() != null) {
                    for (Question q : exam.getQuestions()) {
                        processQuestionForHtmlConversion(q);
                    }
                }

                updateUIFromExam();
                // After import, clear and disable edit pane, reset originalQuestionState
                clearQuestionFields();
                setEditMode(false);
                originalQuestionState = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processQuestionForHtmlConversion(Question question) {
        // Check if it's an MCQ and the text is likely plain text (no <li> tags)
        if ("MCQ".equals(question.getType()) && question.getText() != null && !question.getText().trim().isEmpty() && !question.getText().contains("<li>")) {
            String plainText = question.getText();
            // Split by newline, also handling surrounding whitespace
            String[] lines = plainText.split("\\s*\\n\\s*"); 
            StringBuilder html = new StringBuilder("<ol>");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    html.append("<li>").append(line.trim()).append("</li>");
                }
            }
            html.append("</ol>");
            question.setText(html.toString());
        }
    

        if (question.getSubQuestions() != null) {
            for (Question subQ : question.getSubQuestions()) {
                processQuestionForHtmlConversion(subQ);
            }
        }
    }

    private void updateUIFromExam() {
        examTitleField.setText(exam.getTitle());
        moduleField.setText(exam.getModule());
        semesterField.setText(exam.getSemester());
        fachbereichField.setText(exam.getFachbereich());
        hochschuleField.setText(exam.getHochschule());
        refreshTreeTableView();
    }

    private void updateExamMetadata() {
        exam.setTitle(examTitleField.getText());
        exam.setModule(moduleField.getText());
        exam.setSemester(semesterField.getText());
        exam.setFachbereich(fachbereichField.getText());
        exam.setHochschule(hochschuleField.getText());
    }

    private void updateTotalPoints() {
        totalPointsLabel.setText("Gesamtpunkte: " + exam.getTotalPoints());
    }

    private void clearQuestionFields() {
        questionTitleField.clear();
        questionTextField.setHtmlText("");
        musterloesungField.clear();
        questionPointsField.clear();
        questionTypeField.setValue(null);
        answerLinesField.getValueFactory().setValue(0);
        questionImageView.setImage(null);
        musterloesungImageView.setImage(null);
        newQuestionImageBase64 = null;
        newQuestionSolutionImageBase64 = null;
        parentForSubQuestion = null;
    }

    private Question createVariedQuestionRecursive(Question originalQuestion) {
        String rephrasedTitle = Rephraser.rephrase(originalQuestion.getTitle());
        String rephrasedText = Rephraser.rephrase(originalQuestion.getText());

        Question copiedQuestion = new Question(
                rephrasedTitle,
                rephrasedText,
                originalQuestion.getPoints(),
                originalQuestion.getType(),
                originalQuestion.getAnswerLines()
        );
        copiedQuestion.setMusterloesung(originalQuestion.getMusterloesung());
        copiedQuestion.setId(originalQuestion.getId());
        copiedQuestion.setSelected(originalQuestion.getSelected());
        copiedQuestion.setImageBase64(originalQuestion.getImageBase64()); // Copy image data
        copiedQuestion.setMusterloesungImageBase64(originalQuestion.getMusterloesungImageBase64()); // Copy solution image data


        if (originalQuestion.getSubQuestions() != null && !originalQuestion.getSubQuestions().isEmpty()) {
            List<Question> shuffledSubQuestions = new ArrayList<>();
            for (Question originalSubQuestion : originalQuestion.getSubQuestions()) {
                shuffledSubQuestions.add(createVariedQuestionRecursive(originalSubQuestion));
            }
            Collections.shuffle(shuffledSubQuestions);
            copiedQuestion.setSubQuestions(shuffledSubQuestions);
        }
        return copiedQuestion;
    }

    private boolean areChangesMade() {
        if (originalQuestionState == null) {
            // No question loaded, so no changes to track
            return false;
        }

        // Safely get current points from UI
        int currentPoints = 0;
        try {
            currentPoints = Integer.parseInt(questionPointsField.getText().isEmpty() ? "0" : questionPointsField.getText());
        } catch (NumberFormatException e) {
            // If points field is invalid, consider it a change to prevent data loss
            return true;
        }

        // Compare current UI state with originalQuestionState
        boolean titleChanged = !originalQuestionState.getTitle().equals(questionTitleField.getText());
        boolean textChanged = !originalQuestionState.getText().equals(questionTextField.getHtmlText());
        boolean musterloesungChanged = !originalQuestionState.getMusterloesung().equals(musterloesungField.getText());
        boolean pointsChanged = originalQuestionState.getPoints() != currentPoints; // Use safely parsed points
        boolean typeChanged = !originalQuestionState.getType().equals(questionTypeField.getValue());
        boolean answerLinesChanged = originalQuestionState.getAnswerLines() != answerLinesField.getValue();
        boolean imageChanged = !Objects.equals(originalQuestionState.getImageBase64(), newQuestionImageBase64); // newQuestionImageBase64 holds current image
        boolean solutionImageChanged = !Objects.equals(originalQuestionState.getMusterloesungImageBase64(), newQuestionSolutionImageBase64); // newQuestionSolutionImageBase64 holds current solution image

        return titleChanged || textChanged || musterloesungChanged || pointsChanged || typeChanged || answerLinesChanged || imageChanged || solutionImageChanged;
    }

    private Optional<ButtonType> showUnsavedChangesConfirmation() {
        if (!areChangesMade()) {
            return Optional.empty(); // No changes, no dialog needed
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Ungespeicherte Änderungen");
        alert.setHeaderText("Sie haben ungespeicherte Änderungen an der aktuellen Frage.");
        alert.setContentText("Möchten Sie Ihre Änderungen speichern, bevor Sie fortfahren?");

        ButtonType saveButton = new ButtonType("Änderungen speichern");
        ButtonType continueButton = new ButtonType("Ohne Speichern fortfahren");
        ButtonType cancelButton = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(saveButton, continueButton, cancelButton);

        return alert.showAndWait();
    }

    private String normalizeMcqHtml(String htmlText) {
        if (htmlText == null || htmlText.trim().isEmpty()) {
            return "";
        }

        Document doc = Jsoup.parse(htmlText);
        StringBuilder cleanHtml = new StringBuilder("<ol>");

        // Find all elements that might contain options (p, div, li)
        for (Element element : doc.select("p, div, li")) {
            String text = element.text().trim();
            // Matches "A) Option", "B. Option", "C Option"
            if (text.matches("^[A-Z][). ]\\s*.*")) {
                cleanHtml.append("<li>").append(text).append("</li>");
            }
        }
        cleanHtml.append("</ol>");

        // If no options were found in list format, try to convert plain text lines
        if (cleanHtml.toString().equals("<ol></ol>")) {
            String plainText = doc.body().text(); // Get all text content
            String[] lines = plainText.split("\\s*\\n\\s*");
            cleanHtml = new StringBuilder("<ol>");
            for (String line : lines) {
                if (line.matches("^[A-Z][). ]\\s*.*")) {
                    cleanHtml.append("<li>").append(line.trim()).append("</li>");
                }
            }
            cleanHtml.append("</ol>");
        }

        return cleanHtml.toString();
    }
}
