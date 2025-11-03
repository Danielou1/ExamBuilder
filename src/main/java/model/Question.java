package model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Datenmodell für eine einzelne Frage in der Klausur.
 */
public class Question {
    private UUID id;
    private String title;
    private String text;
    private int points;
    private String type;
    private List<Question> subQuestions;
    private int answerLines;
    private String musterloesung = "";
    private String imageBase64;
    private String musterloesungImageBase64;
    private final BooleanProperty startOnNewPage = new SimpleBooleanProperty(false); // New field for page break

    private final BooleanProperty selected = new SimpleBooleanProperty(true);

    public Question() {
        this.id = UUID.randomUUID();
        this.subQuestions = new ArrayList<>();
        this.startOnNewPage.set(false); // Initialize new field
    }

    public Question(String title, String text, int points, String type, int answerLines) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.text = text;
        this.points = points;
        this.type = type;
        this.answerLines = answerLines;
        this.subQuestions = new ArrayList<>();
        this.startOnNewPage.set(false); // Initialize new field
    }

    public Question(Question other) {
        this.id = other.id; // Keep the same ID for comparison
        this.title = other.title;
        this.text = other.text;
        this.points = other.points;
        this.type = other.type;
        this.answerLines = other.answerLines;
        this.musterloesung = other.musterloesung;
        this.imageBase64 = other.imageBase64;
        this.musterloesungImageBase64 = other.musterloesungImageBase64;
        this.selected.set(other.selected.get()); // Copy the boolean property value
        this.startOnNewPage.set(other.startOnNewPage.get()); // Copy new field
        // Deep copy subQuestions if they exist
        if (other.subQuestions != null) {
            this.subQuestions = new ArrayList<>();
            for (Question subQ : other.subQuestions) {
                this.subQuestions.add(new Question(subQ)); // Recursive copy
            }
        } else {
            this.subQuestions = new ArrayList<>();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Question question = (Question) o;
        return id.equals(question.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- ID Getter/Setter ---
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    // --- JavaFX Property Methods for 'selected' ---

    @JsonIgnore
    public boolean isSelected() {
        return selected.get();
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    // --- Standard Getters/Setters ---

    public boolean getSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public int getAnswerLines() {
        return answerLines;
    }

    public void setAnswerLines(int answerLines) {
        this.answerLines = answerLines;
    }
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getPoints() {
        if (subQuestions != null && !subQuestions.isEmpty()) {
            return subQuestions.stream().mapToInt(Question::getPoints).sum();
        }
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Question> getSubQuestions() {
        return subQuestions;
    }

    public void setSubQuestions(List<Question> subQuestions) {
        this.subQuestions = subQuestions;
    }

    public void addSubQuestion(Question subQuestion) {
        this.subQuestions.add(subQuestion);
    }

    public String getMusterloesung() {
        return musterloesung;
    }

    public void setMusterloesung(String musterloesung) {
        this.musterloesung = musterloesung;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getMusterloesungImageBase64() {
        return musterloesungImageBase64;
    }

    public void setMusterloesungImageBase64(String musterloesungImageBase64) {
        this.musterloesungImageBase64 = musterloesungImageBase64;
    }

    // --- New field getter/setter ---
    @JsonIgnore
    public boolean isStartOnNewPage() {
        return startOnNewPage.get();
    }

    public BooleanProperty startOnNewPageProperty() {
        return startOnNewPage;
    }

    public void setStartOnNewPage(boolean startOnNewPage) {
        this.startOnNewPage.set(startOnNewPage);
    }
}
