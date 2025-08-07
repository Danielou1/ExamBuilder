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

    public static void export(Exam exam, String filePath) {
        try (XWPFDocument document = new XWPFDocument()) {
            // Seite 1: Deckblatt
            createCoverPage(document, exam);

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

    private static void createCoverPage(XWPFDocument document, Exam exam) {
        // Titel der Klausur
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = title.createRun();
        titleRun.setText(exam.getTitle());
        titleRun.setBold(true);
        titleRun.setFontSize(20);

        // Metadaten in einer Tabelle
        XWPFTable metaTable = document.createTable(1, 1);
        metaTable.setWidth("100%");
        XWPFTableRow metaRow = metaTable.getRow(0);
        XWPFParagraph metaParagraph = metaRow.getCell(0).getParagraphs().get(0);
        metaParagraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun metaRun = metaParagraph.createRun();
                metaRun.setText(exam.getHochschule() + " | " + "Fachbereich: " + exam.getFachbereich());
        metaRun.setBold(true);
        metaRun.addBreak();
        metaRun.setText("Modul: " + exam.getModule() + " | " + "Semester: " + exam.getSemester());
        metaRun.setBold(true);

        // Specific instruction phrase in a table
        XWPFTable specificInstructionTable = document.createTable(1, 1);
        specificInstructionTable.setWidth("100%");
        XWPFTableRow instructionTableRow = specificInstructionTable.getRow(0);
        XWPFParagraph instructionTableParagraph = instructionTableRow.getCell(0).getParagraphs().get(0);
        instructionTableParagraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun instructionTableRun = instructionTableParagraph.createRun();
        instructionTableRun.setText("\nBitte lesen Sie die folgenden Hinweise aufmerksam durch!");
        instructionTableRun.setBold(true);

        // Anweisungen
        XWPFParagraph instructions = document.createParagraph();
        XWPFRun instructionsRun = instructions.createRun();
        String instructionsContent = "\nHinweise:\n\u2022 Erg\u00e4nzen Sie bitte auf diesem Deckblatt die untenstehenden Angaben und unterschreiben Sie in dem vorgesehenen Feld (Unterschrift).\n\u2022 Der Klausurbogen enth\u00e4lt ein Zusatzblatt. Weitere Zusatzbl\u00e4tter erhalten Sie bei Bedarf von der Aufsicht. Tragen Sie auf eventuell genutzten weiteren Zusatzbl\u00e4ttern sofort Ihren Nachnamen, die Matrikelnummer und die Aufgabenummer ein.\n\u2022 Verwenden Sie einen dokumentenechten Schreibstift (d. h. kein Bleistift). Verwenden Sie keinen Stift mit roter oder gr\u00fcner Farbe.\n\u2022 Trennen Sie den Klausurbogen nicht auf. Nehmen Sie den Klausurbogen nicht mit nach Hause.\n\u2022 Elektronische und nicht elektronische Hilfsmittel sind nicht zugelassen, mit Ausnahme eines Taschenrechners (kein Smartphone!). Schalten Sie alle mitgebrachten elektronischen Ger\u00e4te \u2013 auch Fitnessarmb\u00e4nder, MP3-Player, etc. \u2013 aus (bzw. komplett lautlos) und legen Sie diese  au\u00dfer Reichweite (z. B. in Ihren Rucksack).\n\u2022 Die Bearbeitungszeit betr\u00e4gt 60 Minuten.\n\u2022 Notieren Sie die Antworten direkt in den Klausurbogen. Der daf\u00fcr vorgesehene Platz ist bei durchschnittlicher Handschriftgr\u00f6\u00dfe ausreichend.\n\u2022 Sie k\u00f6nnen die Klausur jederzeit abgeben. Aus Respekt gegen\u00fcber Ihren Mitstudierenden verlassen Sie bitte 10 Minuten vor dem Ende der Bearbeitungszeit den Klausurraum nicht mehr, um \u00fcberm\u00e4\u00dfige St\u00f6rungen zu vermeiden.\n\nViel Erfolg!";
            String[] lines = instructionsContent.split("\n");
            for (int i = 0; i < lines.length; i++) {
            instructionsRun.setText(lines[i]);
            instructionsRun.setBold(true);
            instructionsRun.setFontSize(11);
            if (i < lines.length - 1) {
                instructionsRun.addBreak();
            }
        }

        // Studenteninformationen
        XWPFParagraph studentInfo = document.createParagraph();
        XWPFRun studentInfoRun = studentInfo.createRun();
        studentInfoRun.setBold(true);
        studentInfoRun.setText("\nAbschnitt: Von dem/der Studierenden auszufüllen");
        studentInfoRun.addBreak();
        studentInfoRun.setText("Name: ______________________________________________");
        studentInfoRun.addBreak();
        studentInfoRun.setText("Vorname: ______________________________________");
        studentInfoRun.addBreak();
        studentInfoRun.setText("Matrikelnummer: ________________");
        studentInfoRun.addBreak();
        studentInfoRun.setText("Unterschrift: ___________________");

        // Notentabelle
        XWPFParagraph gradingInfo = document.createParagraph();
        XWPFRun gradingInfoRun = gradingInfo.createRun();
        gradingInfoRun.setBold(true);
        gradingInfoRun.setText("\nAbschnitt: Von dem/der Prüfenden auszufüllen");

        int numQuestions = exam.getQuestions().size();
        XWPFTable gradingTable = document.createTable(3, numQuestions + 1); // 3 rows, numQuestions + 1 columns
        gradingTable.setWidth("100%");

        // Row 1: Question numbers (A1, A2, ...) and "Gesamt"
        XWPFTableRow headerRow = gradingTable.getRow(0);
        for (int i = 0; i < numQuestions; i++) {
            XWPFRun run = headerRow.getCell(i).getParagraphs().get(0).createRun();
            run.setText("A" + (i + 1));
            run.setBold(true);
        }
        XWPFRun totalHeaderRun = headerRow.getCell(numQuestions).getParagraphs().get(0).createRun();
        totalHeaderRun.setText("Gesamt");
        totalHeaderRun.setBold(true);

        // Row 2: Max points for each question and total max points
        XWPFTableRow maxPointsRow = gradingTable.getRow(1);
        for (int i = 0; i < numQuestions; i++) {
            XWPFRun run = maxPointsRow.getCell(i).getParagraphs().get(0).createRun();
            run.setText(String.valueOf(exam.getQuestions().get(i).getPoints()));
            run.setBold(true);
        }
        XWPFRun totalMaxPointsRun = maxPointsRow.getCell(numQuestions).getParagraphs().get(0).createRun();
        totalMaxPointsRun.setText(String.valueOf(exam.getTotalPoints()));
        totalMaxPointsRun.setBold(true);

        // Row 3: Empty fields for achieved points
        XWPFTableRow achievedPointsRow = gradingTable.getRow(2);
        for (int i = 0; i <= numQuestions; i++) { // Loop up to numQuestions to include the "Gesamt" column
            XWPFRun run = achievedPointsRow.getCell(i).getParagraphs().get(0).createRun();
            run.setText("______");
            run.setBold(true);
        }
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
                continueRun.setText("Fortsetzung der Aufgabe auf der nächsten Seite...");
                continueMessage.setPageBreak(true);
            }
        }
    }

    private static void writeQuestion(XWPFDocument document, Question question, String questionNumber) {
        // Fragentitel
        XWPFParagraph questionTitle = document.createParagraph();
        XWPFRun questionTitleRun = questionTitle.createRun();
        String titleText = question.getTitle() != null && !question.getTitle().isEmpty() ? question.getTitle() + " " : "";
        questionTitleRun.setText("Aufgabe " + questionNumber + ": " + titleText + "(" + question.getPoints() + " Punkte)");
        questionTitleRun.setBold(true);

        // Fragentext
        XWPFParagraph questionText = document.createParagraph();
        XWPFRun questionTextRun = questionText.createRun();
        questionTextRun.setText(question.getText());

        // Add answer lines
        for (int i = 0; i < question.getAnswerLines(); i++) {
            XWPFParagraph answerLine = document.createParagraph();
            XWPFRun answerLineRun = answerLine.createRun();
            answerLineRun.setText("__________________________________________________________________________________");
        }

        // Sub-questions
        if (question.getSubQuestions() != null && !question.getSubQuestions().isEmpty()) {
            for (int i = 0; i < question.getSubQuestions().size(); i++) {
                Question subQuestion = question.getSubQuestions().get(i);
                writeQuestion(document, subQuestion, questionNumber + "." + (char)('a' + i));
            }
        }
    }
}
