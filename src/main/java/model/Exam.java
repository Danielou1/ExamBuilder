package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Datenmodell für die Klausur, enthält eine Liste von Fragen und Metadaten.
 */
public class Exam {
    private String title;
    private String author;
    private String module;
    private String semester;
    private String fachbereich;
    private String hochschule;
    private List<Question> questions;

    public Exam() {
        this.questions = new ArrayList<>();
    }

    public Exam(String title, String author, String module, String semester, String fachbereich, String hochschule) {
        this.title = title;
        this.author = author;
        this.module = module;
        this.semester = semester;
        this.fachbereich = fachbereich;
        this.hochschule = hochschule;
        this.questions = new ArrayList<>();
    }

    // Copy constructor
    public Exam(Exam other) {
        this.title = other.title;
        this.author = other.author;
        this.module = other.module;
        this.semester = other.semester;
        this.fachbereich = other.fachbereich;
        this.hochschule = other.hochschule;
        this.questions = new ArrayList<>(other.questions);
    }

    // Getter und Setter
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getFachbereich() {
        return fachbereich;
    }

    public void setFachbereich(String fachbereich) {
        this.fachbereich = fachbereich;
    }

    public String getHochschule() {
        return hochschule;
    }

    public void setHochschule(String hochschule) {
        this.hochschule = hochschule;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public void addQuestion(Question question) {
        this.questions.add(question);
    }

    public void removeQuestion(Question question) {
        this.questions.remove(question);
    }

    public int getTotalPoints() {
        return questions.stream().mapToInt(this::calculateQuestionPoints).sum();
    }

    private int calculateQuestionPoints(Question question) {
        int points = question.getPoints();
        if (question.getSubQuestions() != null) {
            points += question.getSubQuestions().stream().mapToInt(this::calculateQuestionPoints).sum();
        }
        return points;
    }
}
