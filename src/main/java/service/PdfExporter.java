package service;

import java.io.IOException;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import model.Exam;
import model.Question;

public class PdfExporter {

    public static void export(Exam exam, String filePath, String hilfsmittel) {
        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // Add content to the PDF
            document.add(new Paragraph(exam.getTitle()).setBold().setFontSize(20).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Modul: " + exam.getModule()));
            document.add(new Paragraph("Semester: " + exam.getSemester()));
            document.add(new Paragraph("Fachbereich: " + exam.getFachbereich()));
            document.add(new Paragraph("Hochschule/Uni: " + exam.getHochschule()));
            document.add(new Paragraph("Hilfsmittel: " + hilfsmittel));

            for (int i = 0; i < exam.getQuestions().size(); i++) {
                Question q = exam.getQuestions().get(i);
                writeQuestion(document, q, String.valueOf(i + 1));
            }

            System.out.println("PDF export successful!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeQuestion(Document document, Question question, String questionNumber) {
        document.add(new Paragraph(questionNumber + ". " + question.getTitle() + " (" + question.getPoints() + " Punkte)").setBold());
        if (question.getText() != null && !question.getText().isEmpty()) {
            if ("MCQ".equals(question.getType())) {
                String[] lines = question.getText().split("\r?\n");
                for (String line : lines) {
                    if (line.trim().startsWith("A)") || line.trim().startsWith("B)") || line.trim().startsWith("C)") || line.trim().startsWith("D)")) {
                        document.add(new Paragraph("☐ " + line.trim()));
                    } else {
                        document.add(new Paragraph(line));
                    }
                }
            } else {
                document.add(new Paragraph(question.getText()));
            }
        }

        for (int i = 0; i < question.getAnswerLines(); i++) {
            document.add(new Paragraph("____________________________________________________________________________"));
        }

        if (question.getSubQuestions() != null && !question.getSubQuestions().isEmpty()) {
            for (int i = 0; i < question.getSubQuestions().size(); i++) {
                Question subQuestion = question.getSubQuestions().get(i);
                writeQuestion(document, subQuestion, questionNumber + "." + (char)('a' + i));
            }
        }
    }
}