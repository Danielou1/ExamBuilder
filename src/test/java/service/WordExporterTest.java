package service;

import model.Exam;
import model.Question;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordExporterTest {

    @TempDir
    Path tempDir;
    private Exam exam;

    @BeforeEach
    void setUp() {
        exam = new Exam("Test Exam", "Test Author", "Test Module", "Test Semester", "Test Fachbereich", "Test Hochschule", "Test Hilfsmittel");
        exam.setAllgemeineHinweise("Dies sind allgemeine Hinweise für die Prüfung.");
        exam.setBearbeitungszeit(90);
    }

    private String readDocxContent(File docxFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(docxFile);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    @Test
    void testExportBasicExam() throws IOException {
        Question q1 = new Question("Question 1 Title", "Question 1 Text", 10, "Offene Frage", 5);
        exam.addQuestion(q1);

        File outputFile = tempDir.resolve("basic_exam.docx").toFile();
        WordExporter.export(exam, outputFile.getAbsolutePath());

        assertTrue(outputFile.exists());
        String content = readDocxContent(outputFile);

        assertTrue(content.contains("Test Exam"));
        assertTrue(content.contains("Test Module"));
        assertTrue(content.contains("Test Hochschule"));
        assertTrue(content.contains("Dies sind allgemeine Hinweise für die Prüfung."));
        assertTrue(content.contains("1. Question 1 Title (10 Punkte)"));
        assertTrue(content.contains("Question 1 Text"));
    }

    @Test
    void testExportExamWithSolutions() throws IOException {
        Question q1 = new Question("Question 1 Title", "Question 1 Text", 10, "Offene Frage", 5);
        q1.setMusterloesung("Solution for Q1");
        exam.addQuestion(q1);

        File outputFile = tempDir.resolve("exam_with_solutions.docx").toFile();
        WordExporter.exportWithSolutions(exam, outputFile.getAbsolutePath());

        assertTrue(outputFile.exists());
        String content = readDocxContent(outputFile);

        assertTrue(content.contains("1. Question 1 Title (10 Punkte)"));
        assertTrue(content.contains("Question 1 Text"));
        assertTrue(content.contains("Lösung: Solution for Q1"));
    }

    @Test
    void testExportExamWithMCQ() throws IOException {
        Question q1 = new Question("MCQ Question", "<ol><li>Option A</li><li>Option B</li><li>Option C</li></ol>", 10, "MCQ", 0);
        exam.addQuestion(q1);

        File outputFile = tempDir.resolve("exam_with_mcq.docx").toFile();
        WordExporter.export(exam, outputFile.getAbsolutePath());

        assertTrue(outputFile.exists());
        String content = readDocxContent(outputFile);

        assertTrue(content.contains("1. MCQ Question (10 Punkte)"));
        assertTrue(content.contains("Option A"));
        assertTrue(content.contains("Option B"));
        assertTrue(content.contains("Option C"));
        // For non-solution export, checkboxes should be unchecked
        assertTrue(content.contains("☐ Option A"));
    }

    @Test
    void testExportExamWithMCQAndSolutions() throws IOException {
        Question q1 = new Question("MCQ Question", "<ol><li>Option A</li><li>Option B</li><li>Option C</li></ol>", 10, "MCQ", 0);
        q1.setMusterloesung("A, C");
        exam.addQuestion(q1);

        File outputFile = tempDir.resolve("exam_with_mcq_solutions.docx").toFile();
        WordExporter.exportWithSolutions(exam, outputFile.getAbsolutePath());

        assertTrue(outputFile.exists());
        String content = readDocxContent(outputFile);

        assertTrue(content.contains("1. MCQ Question (10 Punkte)"));
        // Check for the option text, without the checkbox characters
        assertTrue(content.contains("Option A"));
        assertTrue(content.contains("Option B"));
        assertTrue(content.contains("Option C"));
    }

    @Test
    void testExportExamWithLueckentextAndSolutions() throws IOException {
        Question q1 = new Question("Fill-in-the-blank", "This is a ___ test.", 10, "Lückentext", 0);
        q1.setMusterloesung("simple");
        exam.addQuestion(q1);

        File outputFile = tempDir.resolve("exam_with_lueckentext_solutions.docx").toFile();
        WordExporter.exportWithSolutions(exam, outputFile.getAbsolutePath());

        assertTrue(outputFile.exists());
        String content = readDocxContent(outputFile);

        assertTrue(content.contains("1. Fill-in-the-blank (10 Punkte)"));
        assertTrue(content.contains("This is a simple test.")); // Should contain the filled solution
    }

    @Test
    void testExportExamWithRichtigFalschAndSolutions() throws IOException {
        Question q1 = new Question("Statement is true", "", 5, "Richtig/Falsch", 0);
        q1.setMusterloesung("Richtig");
        exam.addQuestion(q1);

        File outputFile = tempDir.resolve("exam_with_richtig_falsch_solutions.docx").toFile();
        WordExporter.exportWithSolutions(exam, outputFile.getAbsolutePath());

        assertTrue(outputFile.exists());
        String content = readDocxContent(outputFile);

        assertTrue(content.contains("1. Statement is true (5 Punkte)"));
        assertTrue(content.contains("☑ Richtig"));
        assertTrue(content.contains("☐ Falsch"));
    }

    @Test
    void testExportExamWithSubQuestions() throws IOException {
        Question mainQ = new Question("Main Question", "Main text", 20, "Offene Frage", 0);
        Question subQ1 = new Question("Sub Question 1", "Sub text 1", 10, "Offene Frage", 3);
        Question subQ2 = new Question("Sub Question 2", "Sub text 2", 10, "Offene Frage", 3);
        mainQ.addSubQuestion(subQ1);
        mainQ.addSubQuestion(subQ2);
        exam.addQuestion(mainQ);

        File outputFile = tempDir.resolve("exam_with_subquestions.docx").toFile();
        WordExporter.export(exam, outputFile.getAbsolutePath());

        assertTrue(outputFile.exists());
        String content = readDocxContent(outputFile);

        assertTrue(content.contains("1. Main Question (10 + 10 = 20 Punkte)"));
        assertTrue(content.contains("1.a. Sub Question 1 (10 Punkte)"));
        assertTrue(content.contains("1.b. Sub Question 2 (10 Punkte)"));
    }

    @Test
    void testExportExamWithPageBreaks() throws IOException {
        Question q1 = new Question("Question 1", "Text 1", 10, "Offene Frage", 0);
        q1.setStartOnNewPage(true); // Should create a blank page before this main question
        exam.addQuestion(q1);

        Question q2 = new Question("Question 2", "Text 2", 10, "Offene Frage", 0);
        exam.addQuestion(q2);

        Question mainQ3 = new Question("Main Question 3", "Main text 3", 20, "Offene Frage", 0);
        Question subQ3a = new Question("Sub Question 3a", "Sub text 3a", 10, "Offene Frage", 0);
        subQ3a.setStartOnNewPage(true); // Should create a page break with message
        mainQ3.addSubQuestion(subQ3a);
        exam.addQuestion(mainQ3);

        File outputFile = tempDir.resolve("exam_with_pagebreaks.docx").toFile();
        WordExporter.export(exam, outputFile.getAbsolutePath());

        assertTrue(outputFile.exists());
        String content = readDocxContent(outputFile);

        // Verifying page breaks by content is tricky with XWPFWordExtractor as it flattens content.
        // We can only assert the presence of content that should be on different pages.
        // For blank pages, it's even harder to assert their existence purely by text content.
        // This test primarily ensures the export process doesn't fail with page break flags.
        assertTrue(content.contains("1. Question 1 (10 Punkte)"));
        assertTrue(content.contains("2. Question 2 (10 Punkte)"));
        assertTrue(content.contains("Die Aufgabe folgt auf der nächsten Seite bzw. Rückseite."));
        assertTrue(content.contains("3.a. Sub Question 3a (10 Punkte)"));
    }

    // Helper to count images in a document (requires more advanced POI usage)
    // This is a placeholder, actual implementation would be more complex.
    private int countImages(File docxFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(docxFile);
             XWPFDocument document = new XWPFDocument(fis)) {
            return document.getAllPictures().size();
        }
    }

    @Test
    void testExportExamWithImages() throws IOException {
        // Create a dummy base64 image string
        String dummyImageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="; // 1x1 transparent PNG

        Question q1 = new Question("Question with Image", "Text", 10, "Offene Frage", 0);
        q1.setImageBase64(dummyImageBase64);
        q1.setMusterloesungImageBase64(dummyImageBase64);
        exam.addQuestion(q1);

        File outputFile = tempDir.resolve("exam_with_images.docx").toFile();
        WordExporter.exportWithSolutions(exam, outputFile.getAbsolutePath());

        assertTrue(outputFile.exists());
    }
}
