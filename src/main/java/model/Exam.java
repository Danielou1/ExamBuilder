package model;

import java.util.ArrayList;
import java.util.List;

public class Exam {
    private String title;
    private String author;
    private String module;
    private String semester;
    private String fachbereich;
    private String hochschule;
    private String hilfsmittel;
    private List<Question> questions;

    public Exam() {
        this.questions = new ArrayList<>();
    }

    public Exam(String title, String author, String module, String semester, String fachbereich, String hochschule, String hilfsmittel) {
        this.title = title;
        this.author = author;
        this.module = module;
        this.semester = semester;
        this.fachbereich = fachbereich;
        this.hochschule = hochschule;
        this.hilfsmittel = hilfsmittel;
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
        this.hilfsmittel = other.hilfsmittel;
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

    public String getHilfsmittel() {
        return hilfsmittel;
    }

    public void setHilfsmittel(String hilfsmittel) {
        this.hilfsmittel = hilfsmittel;
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
        return questions.stream().mapToInt(Question::getPoints).sum();
    }
}