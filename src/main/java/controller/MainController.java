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
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
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

/**
 * Primary controller for the ExamBuilder application's main view.
 * Manages all user interactions, data flow between the UI and the {@link model.Exam} model,
 * and orchestrates core functionalities such as question creation, editing, deletion,
 * exam loading/saving, and exporting to various formats.
 */
public class MainController {

    @FXML
    private Button newQuestionButton;
    @FXML
    private Button addImageButton;
    @FXML
    private Button formatCodeButton;
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
    private TreeTableColumn<Question, String> questionNumberColumn;
    @FXML
    private TreeTableColumn<Question, Boolean> selectedColumn;
    @FXML
    private TreeTableColumn<Question, String> titleColumn;
    @FXML
    private TreeTableColumn<Question, String> typeColumn;
    @FXML
    private TreeTableColumn<Question, String> pointsColumn;
    @FXML
    private TreeTableColumn<Question, Boolean> startOnNewPageColumn;

    @FXML
    private TreeTableColumn<Question, Boolean> justifyColumn;

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
    private CheckBox largeAnswerBoxCheckBox;
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
    private boolean isDirty = false;

    // ButtonTypes for unsaved changes dialog
    private final ButtonType saveButton = new ButtonType("Änderungen speichern");
    private final ButtonType continueButton = new ButtonType("Ohne Speichern fortfahren");
    private final ButtonType cancelButton = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

    /**
     * Initializes the controller after its root element has been completely processed.
     * This method sets up the UI components, binds listeners, configures table columns,
     * loads universities for autocompletion, and initializes the exam state.
     */
    @FXML
    public void initialize() {
        questionsTable.setEditable(true);

        loadUniversities();
        TextFields.bindAutoCompletion(hochschuleField, germanUniversities);

        setupChangeListeners();

        selectedColumn.setEditable(true); // Set to true for CheckBoxTreeTableCell to work
        selectedColumn.setCellValueFactory(param -> {
            if (param.getValue() != null && param.getValue().getValue() != null) {
                return param.getValue().getValue().selectedProperty();
            }
            return null;
        });
        selectedColumn.setCellFactory(CheckBoxTreeTableCell.forTreeTableColumn(selectedColumn));


        startOnNewPageColumn.setEditable(true);
        startOnNewPageColumn.setCellValueFactory(param -> param.getValue().getValue().startOnNewPageProperty());
        startOnNewPageColumn.setCellFactory(CheckBoxTreeTableCell.forTreeTableColumn(startOnNewPageColumn));

        justifyColumn.setEditable(true);
        justifyColumn.setCellValueFactory(param -> param.getValue().getValue().justifyProperty());
        justifyColumn.setCellFactory(CheckBoxTreeTableCell.forTreeTableColumn(justifyColumn));

        questionNumberColumn.setCellValueFactory(param -> new SimpleStringProperty(getQuestionNumber(param.getValue())));

        setupRowFactory();
        setupContextMenu();
        setupImageContextMenu();

        titleColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getTitle()));
        typeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getType()));
        pointsColumn.setCellValueFactory(param -> new SimpleStringProperty(Integer.toString(param.getValue().getValue().getPoints())));

        resetExam(); // Start with a fresh, clean exam

        questionTypeField.getItems().addAll("Offene Frage", "MCQ", "Lückentext", "Richtig/Falsch");

        questionTypeChangeListener = (obs, oldVal, newVal) -> {
            if (isPopulatingUI) {
                return; // Skip listener logic if UI is being populated
            }
            isDirty = true;
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
                                if (!updateQuestionAndReturnSuccess(oldValue)) { // Pass oldValue to the save method
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

        if (formatCodeButton != null) {
            formatCodeButton.setOnAction(event -> {
                javafx.scene.web.WebView webView = (javafx.scene.web.WebView) questionTextField.lookup(".web-view");
                if (webView != null) {
                    javafx.scene.web.WebEngine engine = webView.getEngine();
                    String jsCode = "var sel = window.getSelection(); " +
                                    "if (sel.rangeCount > 0) { " +
                                    "  var range = sel.getRangeAt(0); " +
                                    "  var documentFragment = range.cloneContents(); " +
                                    "  var dummyDiv = document.createElement('div'); " +
                                    "  dummyDiv.appendChild(documentFragment); " +
                                    "  var plainText = dummyDiv.innerText; " +
                                    "  var escapedText = plainText.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;'); " +
                                    "  document.execCommand('insertHTML', false, '<pre style=\"margin: 0;\"><code>' + escapedText + '</code></pre>'); " +
                                    "}";
                    engine.executeScript(jsCode);
                }
            });
        }

        updateQuestionMenuItem.setOnAction(e -> updateQuestionAndReturnSuccess(questionsTable.getSelectionModel().getSelectedItem()));

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
                        isDirty = true;
                    }
                }
            }
        });
    }

    /**
     * Sets up ChangeListeners for various input fields in the UI to track modifications.
     * When a change occurs in any of these fields, the {@code isDirty} flag is set to true,
     * indicating that there are unsaved changes in the current exam or question.
     */
    private void setupChangeListeners() {
        ChangeListener<String> dirtyStringListener = (obs, old, nao) -> isDirty = true;
        ChangeListener<Number> dirtyNumberListener = (obs, old, nao) -> isDirty = true;

        examTitleField.textProperty().addListener(dirtyStringListener);
        moduleField.textProperty().addListener(dirtyStringListener);
        semesterField.textProperty().addListener(dirtyStringListener);
        fachbereichField.textProperty().addListener(dirtyStringListener);
        hochschuleField.textProperty().addListener(dirtyStringListener);

        questionTitleField.textProperty().addListener(dirtyStringListener);
        musterloesungField.textProperty().addListener(dirtyStringListener);
        questionPointsField.textProperty().addListener(dirtyStringListener);
        answerLinesField.valueProperty().addListener(dirtyNumberListener);
    }

    /**
     * Sets up context menus for the question image and solution image views.
     * These context menus allow the user to remove an image by right-clicking on it.
     */
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

    /**
     * Loads a predefined list of German universities into the {@code germanUniversities} list.
     * This list is used for autocompletion in the {@code hochschuleField}.
     */
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

    /**
     * Configures the row factory for the {@code questionsTable}.
     * This setup includes adding change listeners to the {@code selectedProperty},
     * {@code startOnNewPageProperty}, and {@code justifyProperty} of each {@link model.Question}
     * to update the {@code isDirty} flag when these properties are modified.
     * It also applies a CSS style ("deselected-row") to rows that are not selected for export.
     */
    private void setupRowFactory() {
        questionsTable.setRowFactory(tv -> {
            TreeTableRow<Question> row = new TreeTableRow<>() {
                @Override
                protected void updateItem(Question item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().remove("deselected-row");
                    if (item != null && !empty) {
                        ChangeListener<Boolean> dirtyListener = (obs, was, is) -> isDirty = true;
                        item.selectedProperty().addListener(dirtyListener);
                        item.startOnNewPageProperty().addListener(dirtyListener);
                        item.justifyProperty().addListener(dirtyListener);

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

    /**
     * Configures the context menu for the {@code questionsTable}.
     * This menu provides quick actions such as editing, saving changes, deleting,
     * and adding sub-questions. It also dynamically enables or disables
     * menu items based on whether a question is selected and if the edit pane is active.
     */
    private void setupContextMenu() {
        tableContextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Frage bearbeiten");
        editItem.setOnAction(e -> editQuestion());
        MenuItem saveItem = new MenuItem("Änderungen speichern");
        saveItem.setOnAction(e -> updateQuestionAndReturnSuccess(questionsTable.getSelectionModel().getSelectedItem()));
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

    /**
     * Sets the primary stage for this controller and initializes the "Hinweise" (Instructions) dialog.
     * This method is called by the main application class after loading the FXML.
     * @param primaryStage The primary stage of the JavaFX application.
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        createHinweiseDialog();
    }

    /**
     * Loads the FXML for the HinweiseDialog (Instructions Dialog),
     * creates a new modal stage for it, and sets its controller.
     * This dialog allows users to edit general exam instructions and aids.
     */
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

    /**
     * Opens the HinweiseDialog (Instructions Dialog) to allow the user to edit
     * general exam instructions, allowed aids, and exam duration.
     * The current exam data is passed to the dialog, and after the dialog is closed,
     * the {@code isDirty} flag is set to true to indicate potential changes.
     */
    @FXML
    private void openHinweiseDialog() {
        if (hinweiseDialogStage != null) {
            hinweiseDialogController.setData(this.exam);
            hinweiseDialogStage.showAndWait();
            // After the dialog is closed, we assume changes might have been made.
            isDirty = true;
        } else {
            System.err.println("Hinweise Dialog could not be created.");
        }
    }

    /**
     * Toggles the application between normal editing mode and "selection mode" for export.
     * In selection mode, the {@code selectedColumn} of the questions table becomes editable,
     * while other editing controls and actions are disabled. This allows the user
     * to choose which questions to include in the final export.
     */
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

    /**
     * Sets FontAwesome icons for various buttons and menu items in the UI
     * to enhance visual appeal and user experience.
     */
    private void setIcons() {
        newQuestionButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PLUS));
        addImageButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.FILE_IMAGE_ALT));
        addQuestionMenuItem.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PLUS_CIRCLE));
        addSubQuestionMenuItem.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PLUS_SQUARE));
        editQuestionMenuItem.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PENCIL_SQUARE_ALT));
        updateQuestionMenuItem.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.SAVE));
        deleteQuestionMenuItem.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
    }

    /**
     * Installs tooltips for various UI elements to provide helpful descriptions
     * and guide the user on the functionality of buttons and menu items.
     */
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

    /**
     * Enables or disables the question editing pane based on the {@code isEditing} parameter.
     * When the edit mode changes, the state of the action buttons is also updated accordingly.
     * @param isEditing {@code true} to enable edit mode, {@code false} to disable.
     */
    private void setEditMode(boolean isEditing) {
        editPane.setDisable(!isEditing);
        updateActionsState();
    }

    /**
     * Updates the enabled/disabled state of various UI action elements
     * (buttons, menu items) based on the current selection in the questions table
     * and whether the editing pane is active. This ensures that only relevant actions
     * are available to the user at any given time.
     */
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

    /**
     * Initiates the creation of a new question.
     * Before proceeding, it checks for unsaved changes in the currently edited question
     * and prompts the user to save or discard them. If a new question is to be created,
     * it clears the editing fields and prepares the UI for new input.
     */
    @FXML
    private void newQuestion() {
        if (areChangesMade()) { // Check for unsaved changes on the *currently edited* question
            Optional<ButtonType> result = showUnsavedChangesConfirmation();
            if (result.isPresent() && result.get() == cancelButton) {
                return; // User canceled, do not proceed with new question
            }
             if (result.isPresent() && result.get() == saveButton) {
                if (!updateQuestionAndReturnSuccess(questionsTable.getSelectionModel().getSelectedItem())) {
                    return; 
                }
            }
        }
        parentForSubQuestion = null;
        questionsTable.getSelectionModel().clearSelection();
        setEditMode(true);
        clearQuestionFields(); 
        originalQuestionState = null; 
    }

    /**
     * Prepares the UI for editing the currently selected question in the {@code questionsTable}.
     * It populates the editing fields with the selected question's details and
     * activates the edit mode.
     */
    @FXML
    private void editQuestion() {
        TreeItem<Question> selectedItem = questionsTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            parentForSubQuestion = null; 
            populateQuestionDetails(selectedItem.getValue());
            setEditMode(true);
        }
    }

    /**
     * Populates the editing fields (title, text, solution, points, type, etc.)
     * with the details of the provided {@link model.Question} object.
     * It also handles decoding and displaying question and solution images from Base64.
     *
     * @param question The {@link model.Question} object whose details are to be displayed.
     */
    private void populateQuestionDetails(Question question) {
        if (questionTypeChangeListener != null) {
            questionTypeField.valueProperty().removeListener(questionTypeChangeListener);
        }

        isPopulatingUI = true; 
        this.originalQuestionState = new Question(question); 
        questionTitleField.setText(question.getTitle());
        questionTextField.setHtmlText(question.getText());
        musterloesungField.setText(question.getMusterloesung());
        questionPointsField.setText(String.valueOf(question.getPoints()));
        questionTypeField.setValue(question.getType());
        answerLinesField.getValueFactory().setValue(question.getAnswerLines());
        largeAnswerBoxCheckBox.setSelected(question.isLargeAnswerBox());

        newQuestionImageBase64 = question.getImageBase64();
        if (newQuestionImageBase64 != null && !newQuestionImageBase64.isEmpty()) {
            byte[] imageBytes = Base64.getDecoder().decode(newQuestionImageBase64);
            questionImageView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
        } else {
            questionImageView.setImage(null);
        }

        newQuestionSolutionImageBase64 = question.getMusterloesungImageBase64();
        if (newQuestionSolutionImageBase64 != null && !newQuestionSolutionImageBase64.isEmpty()) {
            byte[] imageBytes = Base64.getDecoder().decode(newQuestionSolutionImageBase64);
            musterloesungImageView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
        } else {
            musterloesungImageView.setImage(null);
        }
        isPopulatingUI = false; // Clear flag

        if (questionTypeChangeListener != null) {
            questionTypeField.valueProperty().addListener(questionTypeChangeListener);
        }
    }

    /**
     * Rebuilds the entire {@code questionsTable} from the current {@link model.Exam} data.
     * This method is typically called when the structure of the exam (e.g., questions added, deleted, or reordered)
     * has changed. It attempts to preserve the currently selected question after the refresh.
     */
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
        questionsTable.setShowRoot(false); // Ensure root is not visible

        if (selectedQuestion != null) {
            findAndSelectQuestion(questionsTable.getRoot(), selectedQuestion);
        }
        updateTotalPoints();
    }
    
    /**
     * Recursively searches for and selects a specific {@link model.Question} within the {@code questionsTable}.
     * This is used to restore selection after the table has been refreshed or rebuilt.
     *
     * @param current The current {@code TreeItem<Question>} to start searching from.
     * @param target  The {@link model.Question} object to find and select.
     * @return {@code true} if the target question was found and selected, {@code false} otherwise.
     */
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

    /**
     * Recursively populates a {@code TreeItem} with its sub-questions.
     * This method is used when building or refreshing the {@code questionsTable}
     * to correctly display the hierarchical structure of questions.
     *
     * @param parentItem    The {@code TreeItem<Question>} to which sub-questions will be added.
     * @param parentQuestion The {@link model.Question} object containing the sub-questions.
     */
    private void populateSubQuestions(TreeItem<Question> parentItem, Question parentQuestion) {
        if (parentQuestion.getSubQuestions() != null) {
            for (Question subQ : parentQuestion.getSubQuestions()) {
                TreeItem<Question> subItem = new TreeItem<>(subQ);
                parentItem.getChildren().add(subItem);
                populateSubQuestions(subItem, subQ);
            }
        }
    }

    /**
     * Recursively generates a formatted question number (e.g., "1", "1.a", "1.a.i")
     * for a given {@code TreeItem<Question>} based on its position in the hierarchy.
     *
     * @param item The {@code TreeItem<Question>} for which to generate the number.
     * @return A string representing the formatted question number.
     */
    private String getQuestionNumber(TreeItem<Question> item) {
        if (item == null || item.getParent() == null || item.getParent() == questionsTable.getRoot()) {
            return String.valueOf(questionsTable.getRoot().getChildren().indexOf(item) + 1);
        } else {
            String parentNumber = getQuestionNumber(item.getParent());
            int subIndex = item.getParent().getChildren().indexOf(item);
            char subLetter = (char) ('a' + subIndex);
            return parentNumber + "." + subLetter;
        }
    }

    /**
     * Creates a new {@link model.Question} from the current input fields and adds it
     * to the exam structure. The question can be either a main question or a sub-question
     * depending on the {@code parentForSubQuestion} context.
     * Includes validation for points and clears the UI fields upon successful addition.
     */
    @FXML
    private void addQuestion() {
        if (questionPointsField.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Punktzahl fehlt");
            alert.setHeaderText("Die Punktzahl für diese Frage fehlt.");
            alert.setContentText("Möchten Sie mit 0 Punkten fortfahren oder die Punktzahl manuell eingeben?");

            ButtonType continueBtn = new ButtonType("Fortfahren mit 0 Punkten");
            ButtonType modifyButton = new ButtonType("Manuell eingeben", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(continueBtn, modifyButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == continueBtn) {
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
            parentForSubQuestion = null; 
        } else {
            exam.addQuestion(newQuestion);
        }

        isDirty = true;
        refreshTreeTableView();
        clearQuestionFields();
        setEditMode(false);
    }

    /**
     * Initiates the process of adding a sub-question to the currently selected question.
     * It sets the selected question as the parent context for the new sub-question,
     * clears the editing fields, and enables edit mode for input.
     */
    @FXML
    private void addSubQuestion() {
        TreeItem<Question> selectedItem = questionsTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            parentForSubQuestion = selectedItem;
            questionsTable.getSelectionModel().clearSelection();
            setEditMode(true);
            clearQuestionFields();
        }
    }

    /**
     * Updates an existing {@link model.Question} in the exam model with the data
     * from the UI input fields. This method is called when changes to a question
     * are to be saved. It performs validation, applies the updates to the
     * specified {@code TreeItem}, refreshes the table view, and exits edit mode.
     *
     * @param itemToUpdate The {@code TreeItem<Question>} representing the question
     *                     to be updated. Its associated {@link model.Question} object
     *                     will receive the new data.
     * @return {@code true} if the question was successfully updated, {@code false} otherwise.
     */
    public boolean updateQuestionAndReturnSuccess(TreeItem<Question> itemToUpdate) {
        if (questionPointsField.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Punktzahl fehlt");
            alert.setHeaderText("Die Punktzahl für diese Frage fehlt.");
            alert.setContentText("Möchten Sie mit 0 Punkten fortfahren oder die Punktzahl manuell eingeben?");

            ButtonType continueBtn = new ButtonType("Fortfahren mit 0 Punkten");
            ButtonType modifyButton = new ButtonType("Manuell eingeben", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(continueBtn, modifyButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == continueBtn) {
                questionPointsField.setText("0");
            } else {
                return false; 
            }
        }

        if (isPointsInvalid()) {
            return false;
        }

        TreeItem<Question> selectedItem = itemToUpdate;
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
                questionToUpdate.setStartOnNewPage(selectedItem.getValue().isStartOnNewPage()); 
                questionToUpdate.setJustify(selectedItem.getValue().isJustify());
                questionToUpdate.setLargeAnswerBox(largeAnswerBoxCheckBox.isSelected());
                if (newQuestionImageBase64 != null) {
                    questionToUpdate.setImageBase64(newQuestionImageBase64);
                }
                if (newQuestionSolutionImageBase64 != null) {
                    questionToUpdate.setMusterloesungImageBase64(newQuestionSolutionImageBase64);
                }
                isDirty = true;
                questionsTable.refresh();
                updateTotalPoints();
                this.originalQuestionState = new Question(questionToUpdate);
                populateQuestionDetails(questionToUpdate); 
                setEditMode(false);
                return true; 
            } catch (Exception e) {
                e.printStackTrace();
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Fehler beim Speichern");
                errorAlert.setHeaderText("Die Änderungen konnten nicht gespeichert werden.");
                errorAlert.setContentText("Ein unerwarteter Fehler ist aufgetreten: " + e.getMessage());
                errorAlert.showAndWait();
                return false; 
            }
        } else {
            System.out.println("No item provided to update.");
            return false;
        }
    }

    /**
     * Deletes the currently selected {@link model.Question} from the exam structure
     * after prompting the user for confirmation. It correctly handles the removal
     * of both main questions and sub-questions from their respective parents.
     */
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
                    isDirty = true;
                    refreshTreeTableView();
                    setEditMode(false);
                }
            });
        } else {
            System.out.println("Please select a question to delete.");
        }
    }

    /**
     * Allows the user to select an image file from their file system and
     * attaches it to the currently active question. The image is converted
     * to a Base64 string for storage and displayed in the UI.
     * This method is triggered when the "Bild hinzufügen" (Add Image) button is pressed.
     */
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
                newQuestionImageBase64 = base64String;
                questionImageView.setImage(new Image(new ByteArrayInputStream(fileContent)));
                isDirty = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Removes the image currently associated with the question being edited.
     * Clears the image from display and resets its Base64 representation.
     */
    @FXML
    private void removeImage() {
        newQuestionImageBase64 = null;
        questionImageView.setImage(null);
        isDirty = true;
    }

    /**
     * Allows the user to select an image file from their file system to be used
     * as a solution image for the currently active question. The image is converted
     * to a Base64 string for storage and displayed in the UI.
     */
    @FXML
    private void addSolutionImage() {
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
                newQuestionSolutionImageBase64 = base64String;
                musterloesungImageView.setImage(new Image(new ByteArrayInputStream(fileContent)));
                isDirty = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Removes the solution image currently associated with the question being edited.
     * Clears the solution image from display and resets its Base64 representation.
     */
    @FXML
    private void removeSolutionImage() {
        newQuestionSolutionImageBase64 = null;
        musterloesungImageView.setImage(null);
        isDirty = true;
    }

    /**
     * Checks if the value entered in the {@code questionPointsField} is a valid integer.
     *
     * @return {@code true} if the points value is not a valid integer, {@code false} otherwise.
     */
    private boolean isPointsInvalid() {
        try {
            Integer.parseInt(questionPointsField.getText());
            return false;
        } catch (NumberFormatException e) {
            System.out.println("Points must be a valid number.");
            return true;
        }
    }

    /**
     * Constructs a new {@link model.Question} object using the current values
     * from the UI input fields for title, text, type, points, answer lines,
     * solution, and image data.
     *
     * @return A newly created {@link model.Question} object.
     */
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
        newQuestion.setStartOnNewPage(false); 
        newQuestion.setJustify("Lückentext".equals(type));
        newQuestion.setLargeAnswerBox(largeAnswerBoxCheckBox.isSelected());
        
        if (newQuestionImageBase64 != null) {
            newQuestion.setImageBase64(newQuestionImageBase64);
        }
        if (newQuestionSolutionImageBase64 != null) {
            newQuestion.setMusterloesungImageBase64(newQuestionSolutionImageBase64);
        }

        return newQuestion;
    }

    /**
     * Prepares a list of {@link model.Question} objects for export.
     * It filters the questions based on their {@code selected} property. If no questions
     * are explicitly selected, it prompts the user whether to export all questions
     * or cancel the export operation.
     *
     * @return A list of {@link model.Question} objects to be exported, or {@code null} if the export is cancelled.
     */
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

    /**
     * Recursively filters a list of {@link model.Question} objects, returning a new
     * list containing only the questions (and their sub-questions) that have
     * their {@code selected} property set to {@code true}.
     *
     * @param questions The list of {@link model.Question} objects to filter.
     * @return A new list containing only the selected questions and their selected sub-questions.
     */
    private List<Question> filterSelected(List<Question> questions) {
        List<Question> selected = new ArrayList<>();
        for (Question q : questions) {
            if (q.isSelected()) {
                Question copy = new Question(q);

                if (q.getSubQuestions() != null && !q.getSubQuestions().isEmpty()) {
                    copy.setSubQuestions(filterSelected(q.getSubQuestions()));
                }
                selected.add(copy);
            }
        }
        return selected;
    }

    /**
     * Exports the current exam (or selected questions) to a Microsoft Word (.docx) document.
     * It first updates the exam metadata, then determines which questions to export
     * (all or only selected ones), prompts the user for a save location, and
     * performs the export in a background task while showing a loading indicator.
     */
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

    /**
     * Exports the answer key for the current exam (or selected questions) to
     * a Microsoft Word (.docx) document. Similar to {@link #exportToWord()},
     * it updates metadata, handles question selection, prompts for a save location,
     * and executes the export in a background task with a loading indicator.
     */
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

    /**
     * Saves the current exam data to a JSON file chosen by the user.
     * This method calls {@link #saveExamToJsonWithResult()} to perform the actual save operation.
     */
    @FXML
    private void saveExamToJson() {
        saveExamToJsonWithResult();
    }

    /**
     * Updates the exam metadata from the UI fields, prompts the user for a file
     * save location, and then serializes the entire {@link model.Exam} object
     * to a JSON file. The {@code isDirty} flag is reset upon successful save.
     *
     * @return {@code true} if the exam was successfully saved to a JSON file, {@code false} otherwise.
     */
    private boolean saveExamToJsonWithResult() {
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
                isDirty = false;
                System.out.println("Exam saved to JSON: " + file.getAbsolutePath());
                return true;
            } else {
                return false; 
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates and exports a varied version of the exam. This involves
     * rephrasing question texts using the {@link utils.Rephraser} utility
     * and shuffling the order of sub-questions (if no page breaks are present).
     * The user is prompted to save the varied exam as either a Word document or a JSON file.
     * The operation is performed in a background task with a loading indicator.
     */
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

    /**
     * Imports exam data from a JSON file selected by the user.
     * Before importing, it checks for unsaved changes in the current exam
     * and prompts the user to save or discard them. It also includes logic
     * to process questions from older JSON formats to ensure compatibility
     * with the {@code HTMLEditor}.
     */
    @FXML
    private void importExamFromJson() {
        if (isDirty) {
            ButtonType saveBtn = new ButtonType("Ja, speichern", ButtonBar.ButtonData.YES);
            ButtonType discardBtn = new ButtonType("Nein, verwerfen", ButtonBar.ButtonData.NO);
            ButtonType cancelBtn = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Ungespeicherte Änderungen");
            alert.setHeaderText("Möchten Sie die aktuellen Änderungen speichern, bevor Sie importieren?");
            alert.getButtonTypes().setAll(saveBtn, discardBtn, cancelBtn);

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent()) {
                if (result.get() == saveBtn) {
                    if (!saveExamToJsonWithResult()) {
                        return; 
                    }
                } else if (result.get() == cancelBtn) {
                    return; 
                }
            } else {
                return;
            }
        }

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

                if (exam.getQuestions() != null) {
                    for (Question q : exam.getQuestions()) {
                        processQuestionForHtmlConversion(q);
                    }
                }

                updateUIFromExam();
                clearQuestionFields();
                setEditMode(false);
                originalQuestionState = null;
                isDirty = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Recursively processes a {@link model.Question} (and its sub-questions)
     * to convert plain text MCQ options into an HTML list format.
     * This ensures compatibility with the {@code HTMLEditor} and proper rendering
     * in the exported Word document, especially for older JSON imports.
     *
     * @param question The {@link model.Question} object to process.
     */
    private void processQuestionForHtmlConversion(Question question) {
        if ("MCQ".equals(question.getType()) && question.getText() != null && !question.getText().trim().isEmpty() && !question.getText().contains("<li>")) {
            String plainText = question.getText();
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

    /**
     * Updates the main UI fields (exam metadata and the questions table)
     * with the data from the currently loaded {@link model.Exam} object.
     */
    private void updateUIFromExam() {
        examTitleField.setText(exam.getTitle());
        moduleField.setText(exam.getModule());
        semesterField.setText(exam.getSemester());
        fachbereichField.setText(exam.getFachbereich());
        hochschuleField.setText(exam.getHochschule());
        refreshTreeTableView();
    }

    /**
     * Updates the metadata fields of the current {@link model.Exam} object
     * (title, module, semester, fachbereich, hochschule) with the values
     * from the corresponding UI input fields.
     */
    private void updateExamMetadata() {
        exam.setTitle(examTitleField.getText());
        exam.setModule(moduleField.getText());
        exam.setSemester(semesterField.getText());
        exam.setFachbereich(fachbereichField.getText());
        exam.setHochschule(hochschuleField.getText());
    }

    /**
     * Calculates the total points for the entire exam by summing the points
     * of all top-level questions and updates the {@code totalPointsLabel} in the UI.
     */
    private void updateTotalPoints() {
        totalPointsLabel.setText("Gesamtpunkte: " + exam.getTotalPoints());
    }

    /**
     * Clears all input fields and image displays in the question editing pane.
     * This prepares the UI for entering details of a new question.
     */
    private void clearQuestionFields() {
        questionTitleField.clear();
        questionTextField.setHtmlText("");
        musterloesungField.clear();
        questionPointsField.clear();
        questionTypeField.setValue(null);
        answerLinesField.getValueFactory().setValue(0);
        largeAnswerBoxCheckBox.setSelected(false);
        questionImageView.setImage(null);
        musterloesungImageView.setImage(null);
        newQuestionImageBase64 = null;
        newQuestionSolutionImageBase64 = null;
    }

    /**
     * Recursively creates a varied version of a given {@link model.Question}.
     * This involves rephrasing the question's title and text using the
     * {@link utils.Rephraser} utility and optionally shuffling its sub-questions.
     * Sub-questions are shuffled only if none of them are marked to start on a new page.
     *
     * @param originalQuestion The {@link model.Question} to create a varied copy of.
     * @return A new {@link model.Question} object representing the varied version.
     */
    private Question createVariedQuestionRecursive(Question originalQuestion) {
        Question copiedQuestion = new Question(originalQuestion);

        String rephrasedTitle = Rephraser.rephrase(originalQuestion.getTitle());
        String rephrasedText = Rephraser.rephrase(originalQuestion.getText());
        copiedQuestion.setTitle(rephrasedTitle);
        copiedQuestion.setText(rephrasedText);

        if (originalQuestion.getSubQuestions() != null && !originalQuestion.getSubQuestions().isEmpty()) {
            List<Question> processedSubQuestions = new ArrayList<>();
            boolean containsPageBreak = false;
            for (Question originalSubQuestion : originalQuestion.getSubQuestions()) {
                processedSubQuestions.add(createVariedQuestionRecursive(originalSubQuestion));
                if (originalSubQuestion.isStartOnNewPage()) {
                    containsPageBreak = true;
                }
            }

            if (!containsPageBreak) {
                Collections.shuffle(processedSubQuestions);
            }

            copiedQuestion.setSubQuestions(processedSubQuestions);
        }
        return copiedQuestion;
    }

    /**
     * Determines if there are unsaved changes in the currently edited question.
     * For an existing question, it compares the current UI input fields against
     * the {@code originalQuestionState}. For a new question (when {@code originalQuestionState} is null),
     * it checks if any content has been entered in the editing fields, including
     * handling the {@code HTMLEditor}'s empty HTML structure.
     *
     * @return {@code true} if changes have been made, {@code false} otherwise.
     */
    private boolean areChangesMade() {
        if (originalQuestionState == null) {
            // HTMLEditor returns a full HTML document structure even when empty.
            // We need to check if the body of this HTML contains any actual text.
            String htmlContent = questionTextField.getHtmlText();
            boolean hasVisibleTextInEditor = !Jsoup.parse(htmlContent).body().text().trim().isEmpty();

            return !questionTitleField.getText().isEmpty() ||
                hasVisibleTextInEditor ||
                !musterloesungField.getText().isEmpty() ||
                !questionPointsField.getText().isEmpty();
        }

        int currentPoints = 0;
        try {
            currentPoints = Integer.parseInt(questionPointsField.getText().isEmpty() ? "0" : questionPointsField.getText());
        } catch (NumberFormatException e) {
            return true;
        }

        boolean titleChanged = !Objects.equals(originalQuestionState.getTitle(), questionTitleField.getText());
        boolean textChanged = !Objects.equals(originalQuestionState.getText(), questionTextField.getHtmlText());
        boolean musterloesungChanged = !Objects.equals(originalQuestionState.getMusterloesung(), musterloesungField.getText());
        boolean pointsChanged = originalQuestionState.getPoints() != currentPoints;
        boolean typeChanged = !Objects.equals(originalQuestionState.getType(), questionTypeField.getValue());
        boolean answerLinesChanged = originalQuestionState.getAnswerLines() != answerLinesField.getValue();
        boolean imageChanged = !Objects.equals(originalQuestionState.getImageBase64(), newQuestionImageBase64);
        boolean solutionImageChanged = !Objects.equals(originalQuestionState.getMusterloesungImageBase64(), newQuestionSolutionImageBase64);

        return titleChanged || textChanged || musterloesungChanged || pointsChanged || typeChanged || answerLinesChanged || imageChanged || solutionImageChanged;
    }

    /**
     * Displays a confirmation dialog to the user when unsaved changes are detected
     * in the currently edited question. It prompts the user to either save the changes,
     * continue without saving (discard changes), or cancel the current action.
     *
     * @return An {@code Optional<ButtonType>} representing the user's choice.
     */
    private Optional<ButtonType> showUnsavedChangesConfirmation() {
        if (!areChangesMade()) {
            return Optional.empty(); 
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Ungespeicherte Änderungen");
        alert.setHeaderText("Sie haben ungespeicherte Änderungen an der aktuellen Frage.");
        alert.setContentText("Möchten Sie Ihre Änderungen speichern, bevor Sie fortfahren?");

        alert.getButtonTypes().setAll(saveButton, continueButton, cancelButton);

        return alert.showAndWait();
    }

    /**
     * Displays a confirmation dialog to the user when unsaved changes are detected
     * in the overall exam. It prompts the user to either save the changes,
     * continue without saving (discard changes), or cancel the current action.
     *
     * @return An {@code Optional<ButtonType>} representing the user's choice.
     */
    private Optional<ButtonType> showUnsavedExamChangesConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Ungespeicherte Änderungen an der Prüfung");
        alert.setHeaderText("Es gibt ungespeicherte Änderungen an der gesamten Prüfung.");
        alert.setContentText("Möchten Sie Ihre Änderungen speichern, bevor Sie fortfahren?");

        alert.getButtonTypes().setAll(saveButton, continueButton, cancelButton);

        return alert.showAndWait();
    }


    /**
     * Processes HTML content specifically for Multiple Choice Questions (MCQ).
     * It attempts to extract list-like items (e.g., "A) Option 1", "B. Option 2", "C Option 3")
     * from the raw HTML and formats them into a clean ordered HTML list (`<ol><li>...</li></ol>`).
     * This ensures consistent rendering and processing for MCQ options.
     *
     * @param htmlText The raw HTML string from the {@code HTMLEditor}.
     * @return A normalized HTML string formatted as an ordered list for MCQ options.
     */
    private String normalizeMcqHtml(String htmlText) {
        if (htmlText == null || htmlText.trim().isEmpty()) {
            return "";
        }

        Document doc = Jsoup.parse(htmlText);
        StringBuilder cleanHtml = new StringBuilder("<ol>");

        for (Element element : doc.select("p, div, li")) {
            String text = element.text().trim();
            if (text.matches("^[A-Z][). ]\\s*.*")) {
                cleanHtml.append("<li>").append(text).append("</li>");
            }
        }
        cleanHtml.append("</ol>");

        if (cleanHtml.toString().equals("<ol></ol>")) {
            String plainText = doc.body().text();
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

    /**
     * Initiates a new, empty exam.
     * Before creating a new exam, it checks for any unsaved changes in the
     * current exam and prompts the user to save, discard, or cancel the action.
     * If proceeding, the application's state is reset to a fresh exam.
     */
    @FXML
    private void newExam() {
        if (isDirty) {
            ButtonType saveBtn = new ButtonType("Ja, speichern", ButtonBar.ButtonData.YES);
            ButtonType discardBtn = new ButtonType("Nein, verwerfen", ButtonBar.ButtonData.NO);
            ButtonType cancelBtn = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Ungespeicherte Änderungen");
            alert.setHeaderText("Möchten Sie die aktuellen Änderungen speichern, bevor Sie eine neue Prüfung erstellen?");
            alert.getButtonTypes().setAll(saveBtn, discardBtn, cancelBtn);

            Optional<ButtonType> result = alert.showAndWait();
            
            if (result.isPresent()) {
                if (result.get() == saveBtn) {
                    boolean savedSuccessfully = saveExamToJsonWithResult();
                    if (savedSuccessfully) {
                        resetExam();
                    }
                } else if (result.get() == discardBtn) {
                    resetExam();
                } 
            }
        } else {
            resetExam();
        }
    }

    /**
     * Resets the application to a fresh, empty exam state.
     * This involves creating a new {@link model.Exam} object, updating the UI
     * to reflect the empty state, clearing all question editing fields,
     * disabling edit mode, and resetting dirty flags.
     */
    private void resetExam() {
        exam = new Exam("", "", "", "", "", "", "");
        updateUIFromExam();
        clearQuestionFields();
        setEditMode(false);
        originalQuestionState = null;
        isDirty = false;
        updateTotalPoints();
    }
}
