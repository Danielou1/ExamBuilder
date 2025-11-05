package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

class ExamTest {

    @Test
    void testExamConstructorAndGetters() {
        Exam exam = new Exam("Title", "Author", "Module", "Semester", "Fachbereich", "Hochschule", "Hilfsmittel");
        assertEquals("Title", exam.getTitle());
        assertEquals("Author", exam.getAuthor());
        assertEquals("Module", exam.getModule());
        assertEquals("Semester", exam.getSemester());
        assertEquals("Fachbereich", exam.getFachbereich());
        assertEquals("Hochschule", exam.getHochschule());
        assertEquals("Hilfsmittel", exam.getHilfsmittel());
        assertTrue(exam.getQuestions().isEmpty());
        // New assertions for default values
        assertNull(exam.getAllgemeineHinweise());
        assertEquals(0, exam.getBearbeitungszeit());
    }

    @Test
    void testCopyConstructor() {
        Exam original = new Exam("Title", "Author", "Module", "Semester", "Fachbereich", "Hochschule", "Hilfsmittel");
        original.setAllgemeineHinweise("Some general instructions.");
        original.setBearbeitungszeit(120);
        Question q1 = new Question("Q1", "Text1", 10, "Type1", 5);
        original.addQuestion(q1);

        Exam copy = new Exam(original);

        assertEquals(original.getTitle(), copy.getTitle());
        assertEquals(original.getAuthor(), copy.getAuthor());
        assertEquals(original.getAllgemeineHinweise(), copy.getAllgemeineHinweise());
        assertEquals(original.getBearbeitungszeit(), copy.getBearbeitungszeit());
        assertEquals(original.getQuestions().size(), copy.getQuestions().size());
        // Deep copy check for questions (important for mutable objects)
        assertNotSame(original.getQuestions(), copy.getQuestions()); // Ensure it's a new list
        assertEquals(original.getQuestions().get(0), copy.getQuestions().get(0)); // Ensure content is same
        assertNotSame(original.getQuestions().get(0), copy.getQuestions().get(0)); // Ensure it's a new Question object
    }

    @Test
    void testSetters() {
        Exam exam = new Exam();
        exam.setTitle("New Title");
        exam.setAuthor("New Author");
        exam.setModule("New Module");
        exam.setSemester("New Semester");
        exam.setFachbereich("New Fachbereich");
        exam.setHochschule("New Hochschule");
        exam.setHilfsmittel("New Hilfsmittel");
        exam.setAllgemeineHinweise("Updated general instructions.");
        exam.setBearbeitungszeit(180);

        assertEquals("New Title", exam.getTitle());
        assertEquals("New Author", exam.getAuthor());
        assertEquals("New Module", exam.getModule());
        assertEquals("New Semester", exam.getSemester());
        assertEquals("New Fachbereich", exam.getFachbereich());
        assertEquals("New Hochschule", exam.getHochschule());
        assertEquals("New Hilfsmittel", exam.getHilfsmittel());
        assertEquals("Updated general instructions.", exam.getAllgemeineHinweise());
        assertEquals(180, exam.getBearbeitungszeit());
    }

    @Test
    void testAddAndRemoveQuestion() {
        Exam exam = new Exam();
        Question question = new Question();
        exam.addQuestion(question);
        assertEquals(1, exam.getQuestions().size());
        exam.removeQuestion(question);
        assertTrue(exam.getQuestions().isEmpty());
    }

    @Test
    void testGetTotalPoints() {
        Exam exam = new Exam();
        Question q1 = new Question("Q1", "Text1", 10, "Type1", 5);
        Question q2 = new Question("Q2", "Text2", 15, "Type2", 6);
        exam.addQuestion(q1);
        exam.addQuestion(q2);

        assertEquals(25, exam.getTotalPoints());
    }

    @Test
    void testSetQuestions() {
        Exam exam = new Exam();
        List<Question> questions = new ArrayList<>();
        questions.add(new Question());
        exam.setQuestions(questions);
        assertEquals(1, exam.getQuestions().size());
    }
}
