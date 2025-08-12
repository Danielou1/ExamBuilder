package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Datenmodell für eine einzelne Frage in der Klausur.
 */
public class Question {
    private String title;
    private String text;
    private int points;
    private String type;
    private List<Question> subQuestions;

    private int answerLines;

    public Question() {
        this.subQuestions = new ArrayList<>();
    }

    public Question(String title, String text, int points, String type, int answerLines) {
        this.title = title;
        this.text = text;
        this.points = points;
        this.type = type;
        this.answerLines = answerLines;
        this.subQuestions = new ArrayList<>();
    }

    // Getter und Setter
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
}
