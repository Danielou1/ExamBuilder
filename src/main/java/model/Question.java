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

    private final BooleanProperty selected = new SimpleBooleanProperty(true);

    public Question() {
        this.id = UUID.randomUUID();
        this.subQuestions = new ArrayList<>();
    }

    public Question(String title, String text, int points, String type, int answerLines) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.text = text;
        this.points = points;
        this.type = type;
        this.answerLines = answerLines;
        this.subQuestions = new ArrayList<>();
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
}
