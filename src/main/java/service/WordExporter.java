package service;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;
import org.apache.poi.wp.usermodel.HeaderFooterType;

import model.Exam;
import model.Question;

public class WordExporter {

    private static final String STANDARD_HINWEISE = "\nHinweise:\n" +
            "\u2022 Erg\u00e4nzen Sie bitte auf diesem Deckblatt die untenstehenden Angaben und unterschreiben Sie in dem vorgesehenen Feld (Unterschrift).\n" +
            "\u2022 Der Klausurbogen enth\u00e4lt ein Zusatzblatt. Weitere Zusatzbl\u00e4tter erhalten Sie bei Bedarf von der Aufsicht. Tragen Sie auf eventuell genutzten weiteren Zusatzbl\u00e4ttern sofort Ihren Nachnamen, die Matrikelnummer und die Aufgabenummer ein.\n" +
            "\u2022 Verwenden Sie einen dokumentenechten Schreibstift (d. h. kein Bleistift). Verwenden Sie keinen Stift mit roter oder gr\u00fcner Farbe.\n" +
            "\u2022 Trennen Sie den Klausurbogen nicht auf. Nehmen Sie den Klausurbogen nicht mit nach Hause.\n" +
            "\u2022 Elektronische und nicht elektronische Hilfsmittel sind nicht zugelassen, mit Ausnahme eines Taschenrechners (kein Smartphone!). Schalten Sie alle mitgebrachten elektronischen Ger\u00e4te \u2013 auch Fitnessarmb\u00e4nder, MP3-Player, etc. \u2013 aus (bzw. komplett lautlos) und legen Sie diese  au\u00dfer Reichweite (z. B. in Ihren Rucksack).\n" +
            "\u2022 Die Bearbeitungszeit betr\u00e4gt 60 Minuten.\n" +
            "\u2022 Notieren Sie die Antworten direkt in den Klausurbogen. Der daf\u00fcr vorgesehene Platz ist bei durchschnittlicher Handschriftgr\u00f6\u00dfe ausreichend.\n" +
            "\u2022 Sie k\u00f6nnen die Klausur jederzeit abgeben. Aus Respekt gegen\u00fcber Ihren Mitstudierenden verlassen Sie bitte 10 Minuten vor dem Ende der Bearbeitungszeit den Klausurraum nicht mehr, um \u00fcberm\u00e4\u00dfige St\u00f6rungen zu vermeiden.\n" +
            "\nViel Erfolg!";

    public static String getStandardHinweise() {
        return STANDARD_HINWEISE;
    }

    public static void export(Exam exam, String filePath) {
        exportDoc(exam, filePath, false);
    }

    public static void exportWithSolutions(Exam exam, String filePath) {
        exportDoc(exam, filePath, true);
    }

    private static void exportDoc(Exam exam, String filePath, boolean withSolutions) {
        try (XWPFDocument document = new XWPFDocument()) {
            createCoverPage(document, exam);
            document.createParagraph().setPageBreak(true);
            createQuestionsPage(document, exam, withSolutions);
            createPageNumbering(document);

            try (FileOutputStream out = new FileOutputStream(filePath)) {
                document.write(out);
            }
            System.out.println("Export erfolgreich!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createPageNumbering(XWPFDocument document) {
        XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
        XWPFParagraph paragraph = footer.getParagraphArray(0);
        if (paragraph == null) {
            paragraph = footer.createParagraph();
        }
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setText("Seite ");
        paragraph.getCTP().addNewR().addNewFldChar().setFldCharType(STFldCharType.BEGIN);
        paragraph.getCTP().addNewR().addNewInstrText().setStringValue("PAGE");
        paragraph.getCTP().addNewR().addNewFldChar().setFldCharType(STFldCharType.END);
        run = paragraph.createRun();
        run.setText(" / ");
        paragraph.getCTP().addNewR().addNewFldChar().setFldCharType(STFldCharType.BEGIN);
        paragraph.getCTP().addNewR().addNewInstrText().setStringValue("NUMPAGES");
        paragraph.getCTP().addNewR().addNewFldChar().setFldCharType(STFldCharType.END);
    }

    private static void createCoverPage(XWPFDocument document, Exam exam) {
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = title.createRun();
        titleRun.setText(exam.getTitle());
        titleRun.setBold(true);
        titleRun.setFontSize(20);

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

        document.createParagraph();

        XWPFTable specificInstructionTable = document.createTable(1, 1);
        specificInstructionTable.setWidth("100%");
        XWPFTableRow instructionTableRow = specificInstructionTable.getRow(0);
        XWPFParagraph instructionTableParagraph = instructionTableRow.getCell(0).getParagraphs().get(0);
        instructionTableParagraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun instructionTableRun = instructionTableParagraph.createRun();
        instructionTableRun.setText("\nPlease lesen Sie die folgenden Hinweise aufmerksam durch!");
        instructionTableRun.setBold(true);

        XWPFParagraph instructions = document.createParagraph();
        XWPFRun instructionsRun = instructions.createRun();
        
        String instructionsContent = exam.getAllgemeineHinweise();
        if (instructionsContent == null || instructionsContent.isEmpty()) {
            instructionsContent = getStandardHinweise();
        }

        String[] lines = instructionsContent.split("\n");
        for (int i = 0; i < lines.length; i++) {
            instructionsRun.setText(lines[i]);
            instructionsRun.setBold(true);
            instructionsRun.setFontSize(10);
            if (i < lines.length - 1) {
                instructionsRun.addBreak();
            }
        }

        XWPFParagraph studentInfo = document.createParagraph();
        studentInfo.setSpacingBefore(200);
        XWPFRun studentInfoRun = studentInfo.createRun();
        studentInfoRun.setFontSize(10);
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

        XWPFParagraph gradingInfo = document.createParagraph();
        gradingInfo.setSpacingBefore(200);
        XWPFRun gradingInfoRun = gradingInfo.createRun();
        gradingInfoRun.setFontSize(10);
        gradingInfoRun.setBold(true);
        gradingInfoRun.setText("\nAbschnitt: Von dem/der Prüfenden auszufüllen");

        int numQuestions = exam.getQuestions().size();
        XWPFTable gradingTable = document.createTable(3, numQuestions + 1);
        gradingTable.setWidth("100%");

        XWPFTableRow headerRow = gradingTable.getRow(0);
        for (int i = 0; i < numQuestions; i++) {
            XWPFRun run = headerRow.getCell(i).getParagraphs().get(0).createRun();
            run.setText("A" + (i + 1));
            run.setBold(true);
        }
        XWPFRun totalHeaderRun = headerRow.getCell(numQuestions).getParagraphs().get(0).createRun();
        totalHeaderRun.setText("Gesamt");
        totalHeaderRun.setBold(true);

        XWPFTableRow maxPointsRow = gradingTable.getRow(1);
        for (int i = 0; i < numQuestions; i++) {
            XWPFRun run = maxPointsRow.getCell(i).getParagraphs().get(0).createRun();
            run.setText(String.valueOf(exam.getQuestions().get(i).getPoints()));
            run.setBold(true);
        }
        XWPFRun totalMaxPointsRun = maxPointsRow.getCell(numQuestions).getParagraphs().get(0).createRun();
        totalMaxPointsRun.setText(String.valueOf(exam.getTotalPoints()));
        totalMaxPointsRun.setBold(true);

        XWPFTableRow achievedPointsRow = gradingTable.getRow(2);
        for (int i = 0; i <= numQuestions; i++) {
            XWPFRun run = achievedPointsRow.getCell(i).getParagraphs().get(0).createRun();
            run.setText("______");
            run.setBold(true);
        }
    }

    private static void createQuestionsPage(XWPFDocument document, Exam exam, boolean withSolutions) {
        for (int i = 0; i < exam.getQuestions().size(); i++) {
            Question q = exam.getQuestions().get(i);
            writeQuestion(document, q, String.valueOf(i + 1), withSolutions);

            if (i < exam.getQuestions().size() - 1) {
                XWPFParagraph continueMessage = document.createParagraph();
                continueMessage.setAlignment(ParagraphAlignment.CENTER);
                continueMessage.setPageBreak(true);
            }
        }
    }

    private static void writeQuestion(XWPFDocument document, Question question, String questionNumber, boolean withSolutions) {
        XWPFParagraph questionTitle = document.createParagraph();
        XWPFRun questionTitleRun = questionTitle.createRun();
        
        String titlePrefix;
        if (questionNumber.contains(".")) {
            titlePrefix = questionNumber.substring(questionNumber.lastIndexOf('.') + 1) + ". ";
        } else {
            titlePrefix = questionNumber + ". ";
        }

        String titleText = question.getTitle() != null && !question.getTitle().isEmpty() ? question.getTitle() + " " : "";
        
        String pointsText;
        if (questionNumber.contains(".")) {
            pointsText = "(" + question.getPoints() + " Punkte)";
        } else {
            if (question.getSubQuestions() != null && !question.getSubQuestions().isEmpty()) {
                String pointsDetail = question.getSubQuestions().stream()
                        .map(q -> String.valueOf(q.getPoints()))
                        .collect(Collectors.joining(" + "));
                pointsText = "(" + pointsDetail + " = " + question.getPoints() + " Punkte)";
            } else {
                pointsText = "(" + question.getPoints() + " Punkte)";
            }
        }

        questionTitleRun.setText(titlePrefix + titleText + pointsText);
        questionTitleRun.setBold(true);

        if (question.getText() != null && !question.getText().isEmpty()) {
            if ("MCQ".equals(question.getType())) {
                String[] lines = question.getText().split("\r?\n");
                for (String line : lines) {
                    XWPFParagraph questionTextParagraph = document.createParagraph();
                    XWPFRun questionTextRun = questionTextParagraph.createRun();
                    if (line.trim().startsWith("A)") || line.trim().startsWith("B)") || line.trim().startsWith("C)") || line.trim().startsWith("D)")) {
                        questionTextRun.setText("☐ " + line.trim());
                    } else {
                        questionTextRun.setText(line);
                    }
                }
            } else {
                XWPFParagraph questionTextParagraph = document.createParagraph();
                XWPFRun questionTextRun = questionTextParagraph.createRun();
                questionTextRun.setText(question.getText());
            }
        }

        if (question.getImageBase64() != null && !question.getImageBase64().isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(question.getImageBase64());
                int pictureType = XWPFDocument.PICTURE_TYPE_PNG; // Default to PNG

                if (imageBytes.length > 8 && (imageBytes[0] & 0xFF) == 0xFF && (imageBytes[1] & 0xFF) == 0xD8) {
                    pictureType = XWPFDocument.PICTURE_TYPE_JPEG;
                } else if (imageBytes.length > 4 && (imageBytes[0] & 0xFF) == 0x47 && (imageBytes[1] & 0xFF) == 0x49 && (imageBytes[2] & 0xFF) == 0x46) {
                    pictureType = XWPFDocument.PICTURE_TYPE_GIF;
                } else if (imageBytes.length > 2 && (imageBytes[0] & 0xFF) == 0x42 && (imageBytes[1] & 0xFF) == 0x4D) {
                    pictureType = XWPFDocument.PICTURE_TYPE_BMP;
                }

                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.addPicture(new ByteArrayInputStream(imageBytes), pictureType, "image.png", Units.toEMU(400), Units.toEMU(300));

            } catch (InvalidFormatException | IOException e) {
                e.printStackTrace();
            }
        }

        if (withSolutions) {
            if (question.getMusterloesung() != null && !question.getMusterloesung().isEmpty()) {
                XWPFParagraph solutionParagraph = document.createParagraph();
                XWPFRun solutionRun = solutionParagraph.createRun();
                solutionRun.setText("\nLösung: " + question.getMusterloesung());
                solutionRun.setColor("0000FF"); // Blue color for the solution
                solutionRun.setItalic(true);
            }
        } else {
            for (int i = 0; i < question.getAnswerLines(); i++) {
                XWPFParagraph answerLine = document.createParagraph();
                XWPFRun answerLineRun = answerLine.createRun();
                answerLineRun.setText("__________________________________________________________________________________");
            }
        }

        if (question.getSubQuestions() != null && !question.getSubQuestions().isEmpty()) {
            for (int i = 0; i < question.getSubQuestions().size(); i++) {
                Question subQuestion = question.getSubQuestions().get(i);
                writeQuestion(document, subQuestion, questionNumber + "." + (char)('a' + i), withSolutions);
            }
        }
    }
}