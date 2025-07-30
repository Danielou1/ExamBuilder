package service;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import model.Exam;
import model.Question;

public class WordExporter {

    public static void export(Exam exam, String module, String teacher, String semester, String filePath) {
        try (XWPFDocument document = new XWPFDocument()) {
            // Seite 1: Deckblatt
            createCoverPage(document, exam, module, teacher, semester);

            // Leere Seite einfügen
            document.createParagraph().setPageBreak(true);

            // Seite 2: Fragen
            createQuestionsPage(document, exam);

            // Speichern des Dokuments
            try (FileOutputStream out = new FileOutputStream(filePath)) {
                document.write(out);
            }

            System.out.println("Export erfolgreich!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createCoverPage(XWPFDocument document, Exam exam, String module, String teacher, String semester) {
        // Titel der Klausur
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = title.createRun();
        titleRun.setText(exam.getTitle());
        titleRun.setBold(true);
        titleRun.setFontSize(20);

        // Metadaten
        XWPFParagraph meta = document.createParagraph();
        meta.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun metaRun = meta.createRun();
        metaRun.setText("Modul: " + module + " | " + "Lehrperson: " + teacher + " | " + "Semester: " + semester);

        // Anweisungen
        XWPFParagraph instructions = document.createParagraph();
        XWPFRun instructionsRun = instructions.createRun();       
        String instructionsContent = "Bitte lesen Sie die folgenden Hinweise aufmerksam durch!\nHinweise:\n\u2022 Erg\u00e4nzen Sie bitte auf diesem Deckblatt die untenstehenden Angaben und unterschreiben Sie in dem vorgesehenen Feld (Unterschrift).\n\u2022 Der Klausurbogen enth\u00e4lt ein Zusatzblatt. Weitere Zusatzbl\u00e4tter erhalten Sie bei Bedarf von der Aufsicht. Tragen Sie auf eventuell genutzten weiteren Zusatzbl\u00e4ttern sofort Ihren Nachnamen, die Matrikelnummer und die Aufgabenummer ein.\n\u2022 Verwenden Sie einen dokumentenechten Schreibstift (d. h. kein Bleistift). Verwenden Sie keinen Stift mit roter oder gr\u00fcner Farbe.\n\u2022 Trennen Sie den Klausurbogen nicht auf. Nehmen Sie den Klausurbogen nicht mit nach Hause.\n\u2022 Elektronische und nicht elektronische Hilfsmittel sind nicht zugelassen, mit Ausnahme eines Taschenrechners (kein Smartphone!). Schalten Sie alle mitgebrachten elektronischen Ger\u00e4te \u2013 auch Fitnessarmb\u00e4nder, MP3-Player, etc. \u2013 aus (bzw. komplett lautlos) und legen Sie diese  au\u00dfer Reichweite (z. B. in Ihren Rucksack).\n\u2022 Die Bearbeitungszeit betr\u00e4gt 60 Minuten.\n\u2022 Notieren Sie die Antworten direkt in den Klausurbogen. Der daf\u00fcr vorgesehene Platz ist bei durchschnittlicher Handschriftgr\u00f6\u00dfe ausreichend.\n\u2022 Sie k\u00f6nnen die Klausur jederzeit abgeben. Aus Respekt gegen\u00fcber Ihren Mitstudierenden verlassen Sie bitte 10 Minuten vor dem Ende der Bearbeitungszeit den Klausurraum nicht mehr, um \u00fcberm\u00e4\u00dfige St\u00f6rungen zu vermeiden.\n\nViel Erfolg!";
            String[] lines = instructionsContent.split("\n");
            for (int i = 0; i < lines.length; i++) {
            instructionsRun.setText(lines[i]);
            if (i < lines.length - 1) {
                instructionsRun.addBreak();
            }
        }

        // Studenteninformationen
        XWPFParagraph studentInfo = document.createParagraph();
        XWPFRun studentInfoRun = studentInfo.createRun();
        studentInfoRun.setText("\nAbschnitt: Von dem/der Studierenden auszufüllen");
        studentInfoRun.addBreak();
        studentInfoRun.setText("Name: _______________________________");
        studentInfoRun.addBreak();
        studentInfoRun.setText("Vorname: ____________________________");
        studentInfoRun.addBreak();
        studentInfoRun.setText("Matrikelnummer: ___________");
        studentInfoRun.addBreak();
        studentInfoRun.setText("Unterschrift: ________________");

        // Notentabelle
        XWPFParagraph gradingInfo = document.createParagraph();
        XWPFRun gradingInfoRun = gradingInfo.createRun();
        gradingInfoRun.setText("\nAbschnitt: Von dem/der Prüfenden auszufüllen");

        XWPFTable gradingTable = document.createTable(exam.getQuestions().size() + 2, 3);
        gradingTable.setWidth("100%");

        // Header
        XWPFTableRow header = gradingTable.getRow(0);
        header.getCell(0).setText("Aufgabe");
        header.getCell(1).setText("Punkte Max");
        header.getCell(2).setText("Erreichte Punkte");

        // Zeilen für jede Frage
        for (int i = 0; i < exam.getQuestions().size(); i++) {
            Question q = exam.getQuestions().get(i);
            XWPFTableRow row = gradingTable.getRow(i + 1);
            row.getCell(0).setText("A" + (i + 1));
            row.getCell(1).setText(String.valueOf(q.getPoints()));
            row.getCell(2).setText("______");
        }

        // Gesamtpunktzahl
        XWPFTableRow totalRow = gradingTable.getRow(exam.getQuestions().size() + 1);
        totalRow.getCell(0).setText("Gesamt");
        totalRow.getCell(1).setText(String.valueOf(exam.getTotalPoints()));
        totalRow.getCell(2).setText("______");
    }

    private static void createQuestionsPage(XWPFDocument document, Exam exam) {
        for (int i = 0; i < exam.getQuestions().size(); i++) {
            Question q = exam.getQuestions().get(i);
            writeQuestion(document, q, String.valueOf(i + 1));

            // Add "l'examen continue sur la page suivante" message and page break if not the last question
            if (i < exam.getQuestions().size() - 1) {
                XWPFParagraph continueMessage = document.createParagraph();
                continueMessage.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun continueRun = continueMessage.createRun();
                continueRun.setText("L'examen continue sur la page suivante...");
                continueMessage.setPageBreak(true);
            }
        }
    }

    private static void writeQuestion(XWPFDocument document, Question question, String questionNumber) {
        // Fragentitel
        XWPFParagraph questionTitle = document.createParagraph();
        XWPFRun questionTitleRun = questionTitle.createRun();
        questionTitleRun.setText("Aufgabe " + questionNumber + ": " + question.getTitle() + " (" + question.getPoints() + " Punkte)");
        questionTitleRun.setBold(true);

        // Fragentext
        XWPFParagraph questionText = document.createParagraph();
        XWPFRun questionTextRun = questionText.createRun();
        questionTextRun.setText(question.getText());
        questionTextRun.addBreak();

        // Sub-questions
        if (question.getSubQuestions() != null && !question.getSubQuestions().isEmpty()) {
            for (int i = 0; i < question.getSubQuestions().size(); i++) {
                Question subQuestion = question.getSubQuestions().get(i);
                writeQuestion(document, subQuestion, questionNumber + "." + (char)('a' + i));
            }
        }
    }
}
