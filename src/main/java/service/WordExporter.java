package service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc;

import model.Exam;
import model.Question;

/**
 * Service class responsible for exporting an {@link model.Exam} object
 * to a Microsoft Word (.docx) document using Apache POI library.
 * It handles the generation of exam papers, answer keys, and various
 * formatting requirements including HTML content conversion, image embedding,
 * and specific handling for different question types (MCQ, fill-in-the-blank, true/false).
 */
public class WordExporter {

    private static final String STANDARD_HINWEISE = "\nHinweise:\n" +
            "\u2022 Erg\u00e4nzen Sie bitte auf diesem Deckblatt die untenstehenden Angaben und unterschreiben Sie. Der Klausurbogen enth\u00e4lt ein Zusatzblatt; weitere erhalten Sie bei Bedarf von der Aufsicht. Tragen Sie auf allen Zusatzbl\u00e4ttern sofort Ihren Nachnamen, Matrikelnummer und die Aufgabenummer ein.\n" +
            "\u2022 Verwenden Sie einen dokumentenechten Schreibstift (d. h. kein Bleistift). Verwenden Sie keinen Stift mit roter oder gr\u00fcner Farbe.\n" +
            "\u2022 Trennen Sie den Klausurbogen nicht auf und nehmen Sie ihn nicht mit nach Hause. Notieren Sie die Antworten direkt in den Klausurbogen; der daf\u00fcr vorgesehene Platz ist bei durchschnittlicher Handschriftgr\u00f6\u00dfe ausreichend.\n" +
            "\u2022 Elektronische und nicht elektronische Hilfsmittel sind nicht zugelassen, mit Ausnahme eines Taschenrechners (kein Smartphone!). Schalten Sie alle mitgebrachten elektronischen Ger\u00e4te \u2013 auch Fitnessarmb\u00e4nder, MP3-Player, etc. \u2013 aus (bzw. komplett lautlos) und legen Sie diese au\u00dfer Reichweite (z. B. in Ihren Rucksack).\n" +
            "\u2022 Die Bearbeitungszeit betr\u00e4gt 60 Minuten. Sie k\u00f6nnen die Klausur jederzeit abgeben, jedoch bitten wir Sie aus Respekt gegen\u00fcber Ihren Mitstudierenden, den Raum in den letzten 10 Minuten vor dem Ende der Bearbeitungszeit nicht mehr zu verlassen, um \u00fcberm\u00e4\u00dfige St\u00f6rungen zu vermeiden.\n" +
            "\nViel Erfolg!";

    /**
     * Provides a default block of general instructions ("Hinweise") for the exam cover page.
     * This text serves as a fallback if no specific instructions are provided in the {@link model.Exam} object.
     * @return A string containing the standard German exam instructions.
     */
    public static String getStandardHinweise() {
        return STANDARD_HINWEISE;
    }

    /**
     * Exports the given {@link model.Exam} object to a Microsoft Word (.docx) document.
     * This method generates the exam paper without including the solutions.
     * @param exam The {@link model.Exam} object to export.
     * @param filePath The full path where the .docx file will be saved.
     */
    public static void export(Exam exam, String filePath) {
        exportDoc(exam, filePath, false);
    }

    /**
     * Exports the given {@link model.Exam} object to a Microsoft Word (.docx) document,
     * including the solutions for each question.
     * @param exam The {@link model.Exam} object to export.
     * @param filePath The full path where the .docx file will be saved.
     */
    public static void exportWithSolutions(Exam exam, String filePath) {
        exportDoc(exam, filePath, true);
    }

    /**
     * Core method for generating the Word (.docx) document.
     * It orchestrates the creation of the cover page, default headers,
     * question pages, and page numbering.
     * @param exam The {@link model.Exam} object containing all exam data.
     * @param filePath The destination path for the generated .docx file.
     * @param withSolutions {@code true} to include solutions in the document, {@code false} otherwise.
     */
    private static void exportDoc(Exam exam, String filePath, boolean withSolutions) {
        try (XWPFDocument document = new XWPFDocument()) {
            document.getDocument().getBody().addNewSectPr().addNewTitlePg();
            
            // Create a blank header for the first page
            document.createHeader(HeaderFooterType.FIRST);

            // Create the default header for all other pages
            createDefaultHeader(document);

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

    /**
     * Creates a default header for all pages in the Word document, except the first page.
     * The header includes placeholders for student's matriculation number and name.
     * @param document The {@link org.apache.poi.xwpf.usermodel.XWPFDocument} to which the header is added.
     */
    private static void createDefaultHeader(XWPFDocument document) {
        XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
        XWPFTable table = header.createTable(1, 4);
        table.setWidth("100%");
        table.getCTTbl().getTblPr().unsetTblBorders();

        XWPFTableRow row = table.getRow(0);

        // Matrikelnummer field
        XWPFTableCell matrikelLabelCell = row.getCell(0);
        matrikelLabelCell.setText("Matrikelnummer: ");
        matrikelLabelCell.getParagraphs().get(0).setAlignment(ParagraphAlignment.LEFT);

        XWPFTableCell matrikelValueCell = row.getCell(1);
        // Apply shading to the cell
        matrikelValueCell.getCTTc().addNewTcPr().addNewShd().setFill("F0F0F0");
        XWPFRun matrikelRun = matrikelValueCell.getParagraphs().get(0).createRun();
        matrikelRun.setText(" ".repeat(20)); // Add spaces to be underlined
        matrikelRun.setUnderline(org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE);

        // Name field
        XWPFTableCell nameLabelCell = row.getCell(2);
        nameLabelCell.setText("Name: ");
        nameLabelCell.getParagraphs().get(0).setAlignment(ParagraphAlignment.LEFT);

        XWPFTableCell nameValueCell = row.getCell(3);
        // Apply shading to the cell
        nameValueCell.getCTTc().addNewTcPr().addNewShd().setFill("F0F0F0");
        XWPFRun nameRun = nameValueCell.getParagraphs().get(0).createRun();
        nameRun.setText(" ".repeat(30)); // Add spaces to be underlined
        nameRun.setUnderline(org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE);
    }

    /**
     * Adds page numbering to the footer of the Word document in the format "Page X / Y".
     * @param document The {@link org.apache.poi.xwpf.usermodel.XWPFDocument} to which the footer is added.
     */
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

    /**
     * Generates the cover page for the exam document.
     * This includes displaying exam metadata (university, module, title, etc.),
     * general instructions, fields for student information (name, matriculation number, signature),
     * and a grading table for the examiners.
     * @param document The {@link org.apache.poi.xwpf.usermodel.XWPFDocument} to which the cover page is added.
     * @param exam The {@link model.Exam} object containing the exam's metadata and instructions.
     */
    private static void createCoverPage(XWPFDocument document, Exam exam) {
        // Create a 1x1 table to frame the meta information
        XWPFTable metaTable = document.createTable(1, 1);
        metaTable.setWidth("100%");
        // Ensure borders are visible
        metaTable.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
        metaTable.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
        metaTable.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
        metaTable.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");

        XWPFTableCell metaCell = metaTable.getRow(0).getCell(0);
        setCellAlignment(metaCell, ParagraphAlignment.CENTER, STVerticalJc.CENTER);

        // First paragraph for Hochschule and Fachbereich
        XWPFParagraph metaParagraph1 = metaCell.getParagraphs().get(0);
        metaParagraph1.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun metaRun1 = metaParagraph1.createRun();
        metaRun1.setText(exam.getHochschule() + " | " + exam.getFachbereich());
        metaRun1.setBold(true);
        metaRun1.setFontSize(12);

        // Second paragraph for Modul, Semester, and Title
        XWPFParagraph metaParagraph2 = metaCell.addParagraph();
        metaParagraph2.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun metaRun2 = metaParagraph2.createRun();
        metaRun2.setText(exam.getTitle() + " - " + exam.getModule() + " | "  + exam.getSemester() );
        metaRun2.setBold(true);
        metaRun2.setFontSize(12);

        document.createParagraph(); // Keep a paragraph for spacing

        XWPFTable specificInstructionTable = document.createTable(1, 1);
        specificInstructionTable.setWidth("100%");
        setCellAlignment(specificInstructionTable.getRow(0).getCell(0), ParagraphAlignment.CENTER, STVerticalJc.CENTER);
        XWPFTableRow instructionTableRow = specificInstructionTable.getRow(0);
        XWPFParagraph instructionTableParagraph = instructionTableRow.getCell(0).getParagraphs().get(0);
        instructionTableParagraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun instructionTableRun = instructionTableParagraph.createRun();
        instructionTableRun.setText("\nBitte lesen Sie die folgenden Hinweise aufmerksam durch!");
        instructionTableRun.setBold(true);

        String instructionsContent = exam.getAllgemeineHinweise();
        if (instructionsContent == null || instructionsContent.isEmpty()) {
            instructionsContent = getStandardHinweise();
        }

        String[] lines = instructionsContent.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            XWPFParagraph instructionParagraph = document.createParagraph();
            instructionParagraph.setAlignment(ParagraphAlignment.BOTH);
            XWPFRun instructionRun = instructionParagraph.createRun();
            instructionRun.setText(line);
            instructionRun.setBold(true);
            instructionRun.setFontSize(10);
        }

        XWPFParagraph studentInfoHeader = document.createParagraph();
        studentInfoHeader.setSpacingBefore(200);
        XWPFRun studentInfoHeaderRun = studentInfoHeader.createRun();
        studentInfoHeaderRun.setFontSize(10);
        studentInfoHeaderRun.setBold(true);
        studentInfoHeaderRun.setText("\nAbschnitt: Von dem/der Studierenden auszufüllen");

        XWPFTable studentInfoTable = document.createTable(4, 2); 
        studentInfoTable.setWidth("100%");

        // Add borders to the table
        studentInfoTable.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
        studentInfoTable.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
        studentInfoTable.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
        studentInfoTable.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
        studentInfoTable.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
        studentInfoTable.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");

        // Row 1: Name: | (empty for input)
        XWPFTableRow row1 = studentInfoTable.getRow(0);
        row1.getCell(0).setWidth("33%");
        row1.getCell(1).setWidth("67%");
        setCellAlignment(row1.getCell(0), ParagraphAlignment.CENTER, STVerticalJc.CENTER); 
        row1.getCell(0).setText("Name");
        setCellAlignment(row1.getCell(1), ParagraphAlignment.CENTER, STVerticalJc.CENTER); 
        row1.getCell(1).setText(""); 

        // Row 2: Vorname: | (empty for input)
        XWPFTableRow row2 = studentInfoTable.getRow(1);
        row2.getCell(0).setWidth("33%");
        row2.getCell(1).setWidth("67%");
        setCellAlignment(row2.getCell(0), ParagraphAlignment.CENTER, STVerticalJc.CENTER); 
        row2.getCell(0).setText("Vorname");
        setCellAlignment(row2.getCell(1), ParagraphAlignment.CENTER, STVerticalJc.CENTER); 
        row2.getCell(1).setText("");

        // Row 3: Matrikelnummer: | (empty for input)
        XWPFTableRow row3 = studentInfoTable.getRow(2);
        row3.getCell(0).setWidth("33%");
        row3.getCell(1).setWidth("67%");
        setCellAlignment(row3.getCell(0), ParagraphAlignment.CENTER, STVerticalJc.CENTER); 
        row3.getCell(0).setText("Matrikelnummer");
        setCellAlignment(row3.getCell(1), ParagraphAlignment.CENTER, STVerticalJc.CENTER); 
        row3.getCell(1).setText("");

        // Row 4: Unterschrift: | (empty for input)
        XWPFTableRow row4 = studentInfoTable.getRow(3);
        row4.getCell(0).setWidth("33%");
        row4.getCell(1).setWidth("67%");
        setCellAlignment(row4.getCell(0), ParagraphAlignment.CENTER, STVerticalJc.CENTER); 
        row4.getCell(0).setText("Unterschrift");
        setCellAlignment(row4.getCell(1), ParagraphAlignment.CENTER, STVerticalJc.CENTER); 
        row4.getCell(1).setText("");

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
            XWPFTableCell cell = headerRow.getCell(i);
            setCellAlignment(cell, ParagraphAlignment.CENTER, STVerticalJc.CENTER);
            XWPFRun run = cell.getParagraphs().get(0).createRun();
            run.setText("A" + (i + 1));
            run.setBold(true);
        }
        XWPFTableCell totalHeaderCell = headerRow.getCell(numQuestions);
        setCellAlignment(totalHeaderCell, ParagraphAlignment.CENTER, STVerticalJc.CENTER);
        XWPFRun totalHeaderRun = totalHeaderCell.getParagraphs().get(0).createRun();
        totalHeaderRun.setText("Gesamt");
        totalHeaderRun.setBold(true);

        XWPFTableRow maxPointsRow = gradingTable.getRow(1);
        for (int i = 0; i < numQuestions; i++) {
            XWPFTableCell cell = maxPointsRow.getCell(i);
            setCellAlignment(cell, ParagraphAlignment.CENTER, STVerticalJc.CENTER);
            XWPFRun run = cell.getParagraphs().get(0).createRun();
            run.setText(String.valueOf(exam.getQuestions().get(i).getPoints()));
            run.setBold(true);
        }
        XWPFTableCell totalMaxPointsCell = maxPointsRow.getCell(numQuestions);
        setCellAlignment(totalMaxPointsCell, ParagraphAlignment.CENTER, STVerticalJc.CENTER);
        XWPFRun totalMaxPointsRun = totalMaxPointsCell.getParagraphs().get(0).createRun();
        totalMaxPointsRun.setText(String.valueOf(exam.getTotalPoints()));
        totalMaxPointsRun.setBold(true);

        XWPFTableRow achievedPointsRow = gradingTable.getRow(2);
        for (int i = 0; i <= numQuestions; i++) {
            XWPFTableCell cell = achievedPointsRow.getCell(i);
            setCellAlignment(cell, ParagraphAlignment.CENTER, STVerticalJc.CENTER);
            cell.setText("");
        }
    }

    /**
     * Iterates through the main questions of an {@link model.Exam} and writes each to the document.
     * This method implements the logic for default page breaks: each main question
     * (except the first) starts on a new page. An additional page break is inserted
     * if the question's {@code startOnNewPage} property is true, effectively creating a blank page.
     * @param document The {@link org.apache.poi.xwpf.usermodel.XWPFDocument} to which questions are added.
     * @param exam The {@link model.Exam} object containing the questions.
     * @param withSolutions {@code true} to include solutions for questions, {@code false} otherwise.
     */
    private static void createQuestionsPage(XWPFDocument document, Exam exam, boolean withSolutions) {
        for (int i = 0; i < exam.getQuestions().size(); i++) {
            Question q = exam.getQuestions().get(i);

            // Default behavior: Each main question starts on a new page.
            // A page break is added BEFORE the question, but not for the very first one (i=0)
            // as it already follows the cover page's page break.
            if (i > 0) {
                document.createParagraph().setPageBreak(true);
            }

            // "Neue Seite" checkbox logic: If checked, add an EXTRA page break to create a blank page.
            if (q.isStartOnNewPage()) {
                document.createParagraph().setPageBreak(true);
            }

            writeQuestion(document, q, String.valueOf(i + 1), withSolutions, false);
        }
    }

    /**
     * Writes a single {@link model.Question} (and recursively its sub-questions) to the Word document.
     * This method formats the question title, includes points, embeds images,
     * converts HTML content to Word formatting, and applies specific logic
     * for different question types (MCQ, Lückentext, Richtig/Falsch).
     * It also manages answer areas and page breaks for sub-questions.
     *
     * @param document The {@link org.apache.poi.xwpf.usermodel.XWPFDocument} to which the question is added.
     * @param question The {@link model.Question} object to write.
     * @param questionNumber The formatted number of the question (e.g., "1", "1.a").
     * @param withSolutions {@code true} to include solutions, {@code false} otherwise.
     * @param isSubQuestion {@code true} if the current question is a sub-question, {@code false} otherwise.
     */
    private static void writeQuestion(XWPFDocument document, Question question, String questionNumber, boolean withSolutions, boolean isSubQuestion) {
        XWPFParagraph questionTitle = document.createParagraph();
        if (question.isJustify()) {
            questionTitle.setAlignment(ParagraphAlignment.BOTH);
        }
        // Fix for NullPointerException: Ensure the paragraph properties object exists.
        if (questionTitle.getCTP().getPPr() == null) {
            questionTitle.getCTP().addNewPPr();
        }
        questionTitle.getCTP().getPPr().addNewKeepLines().setVal(true); // Prevents the title itself from splitting
        questionTitle.setKeepNext(true);  // Keeps the title with the next paragraph
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

        // Reduce space after the question title for MCQ type questions
        if ("MCQ".equals(question.getType())) {
            questionTitle.setSpacingAfter(0);
        }

        // Handle question image
        if (question.getImageBase64() != null && !question.getImageBase64().isEmpty()) {
            try {
                byte[] imageBytes = Base64.decodeBase64(question.getImageBase64());
                XWPFParagraph imageParagraph = document.createParagraph();
                addScaledPicture(imageParagraph, imageBytes, XWPFDocument.PICTURE_TYPE_PNG, "question_image.png");
            } catch (IOException | InvalidFormatException e) {
                e.printStackTrace();
            }
        }

        List<String> correctOptions = null;
        if (withSolutions && "MCQ".equals(question.getType()) && question.getMusterloesung() != null && !question.getMusterloesung().isEmpty()) {
            correctOptions = Arrays.stream(question.getMusterloesung().toUpperCase().split("[,\\s]+"))
                                 .map(String::trim)
                                 .filter(s -> !s.isEmpty())
                                 .collect(Collectors.toList());
        }

        if ("Lückentext".equals(question.getType()) && withSolutions) {
            handleLueckentextSolution(document, question);
        } else if ("Richtig/Falsch".equals(question.getType())) {
            handleRichtigFalsch(document, question, withSolutions);
        } else if (question.getText() != null && !question.getText().isEmpty()) {
            appendHtml(document, question, withSolutions, correctOptions, null);
        }

        if ("MCQ".equals(question.getType())) {
            XWPFParagraph spacingParagraph = document.createParagraph();
            spacingParagraph.setSpacingAfter(0); // Standard line space
        }

        if (withSolutions) {
            // For non-MCQ and non-Lückentext questions, print the musterloesung text field.
            if (!"MCQ".equals(question.getType()) && !"Lückentext".equals(question.getType()) && question.getMusterloesung() != null && !question.getMusterloesung().isEmpty()) {
                XWPFParagraph solutionParagraph = document.createParagraph();
                XWPFRun solutionRun = solutionParagraph.createRun();
                solutionRun.setText("\nLösung: " + question.getMusterloesung());
                solutionRun.setColor("0000FF"); // Blue color for the solution
                solutionRun.setItalic(true);
            }

            // Handle solution image
            if (withSolutions && question.getMusterloesungImageBase64() != null && !question.getMusterloesungImageBase64().isEmpty()) {
                try {
                    byte[] imageBytes = Base64.decodeBase64(question.getMusterloesungImageBase64());
                    XWPFParagraph imageParagraph = document.createParagraph();
                    addScaledPicture(imageParagraph, imageBytes, XWPFDocument.PICTURE_TYPE_PNG, "solution_image.png");
                } catch (IOException | InvalidFormatException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // Logic for creating the answer area
            if (!"MCQ".equals(question.getType()) && !"Lückentext".equals(question.getType()) && question.getAnswerLines() > 0) {
                XWPFTable answerTable = document.createTable(question.getAnswerLines(), 1);
                answerTable.setWidth("100%");

                if (question.isLargeAnswerBox()) {
                    // For a large answer box, remove the inner horizontal lines
                    answerTable.setInsideHBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "FFFFFF");
                }

                // Create the rows and cells as before; the number of lines still determines the height
                for (int i = 0; i < question.getAnswerLines(); i++) {
                    XWPFTableRow row = answerTable.getRow(i);
                    XWPFTableCell cell = row.getCell(0);
                    setCellAlignment(cell, ParagraphAlignment.CENTER, STVerticalJc.CENTER);
                    // Add a paragraph with a single space to ensure cell height is respected
                    cell.setText(" ");
                }
            }
        }

        if (question.getSubQuestions() != null && !question.getSubQuestions().isEmpty()) {
            for (int i = 0; i < question.getSubQuestions().size(); i++) {
                Question subQuestion = question.getSubQuestions().get(i);
                
                // Write the sub-question first.
                writeQuestion(document, subQuestion, questionNumber + "." + (char)('a' + i), withSolutions, true);

                // Insert page break AFTER the question if the sub-question is marked to start on a new page
                if (subQuestion.isStartOnNewPage()) {
                    // Add continuation message to the previous page
                    XWPFParagraph continueMessage = document.createParagraph();
                    continueMessage.setAlignment(ParagraphAlignment.RIGHT);
                    XWPFRun continueRun = continueMessage.createRun();
                    continueRun.setText("Die Aufgabe folgt auf der nächsten Seite bzw. Rückseite.");
                    continueRun.setItalic(true);
                    continueRun.setFontSize(9);
                    
                    // Insert page break
                    document.createParagraph().setPageBreak(true);
                }
            }
        }
    }

    /**
     * Handles the display of solutions for "Lückentext" (fill-in-the-blank) questions.
     * It replaces the blank placeholders (three or more underscores) in the question text
     * with the corresponding solutions from the {@code musterloesung} field,
     * formatting them in blue and bold. Includes error handling if no solution is provided.
     *
     * @param document The {@link org.apache.poi.xwpf.usermodel.XWPFDocument} to which the content is added.
     * @param question The {@link model.Question} object of type "Lückentext".
     */
    private static void handleLueckentextSolution(XWPFDocument document, Question question) {
        String htmlText = question.getText();
        String musterloesung = question.getMusterloesung();

        if (musterloesung == null || musterloesung.trim().isEmpty()) {
            appendHtml(document, question, false, null, null); // Show blanks, preserving teacher's underscores
            XWPFParagraph p = document.createParagraph();
            XWPFRun run = p.createRun();
            run.setText("FEHLER: Für diesen Lückentext wurde keine Musterlösung angegeben.");
            run.setColor("FF0000");
            run.setItalic(true);
            return;
        }

        String[] solutions = musterloesung.split("\\s*;\\s*");
        int solutionIndex = 0;
        String placeholderRegex = "_{3,}"; // A blank is 3 or more underscores

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(placeholderRegex);
        java.util.regex.Matcher matcher = pattern.matcher(htmlText);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String replacement;
            if (solutionIndex < solutions.length) {
                String solution = solutions[solutionIndex].trim();
                replacement = " <font color=\"0000FF\"><b>" + solution + "</b></font> ";
                solutionIndex++;
            } else {
                // If there are more blanks than solutions, fill the rest with empty styled blanks.
                replacement = " <font color=\"0000FF\"><b></b></font> ";
            }
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        String filledText = sb.toString();

        // Now parse the modified HTML and append it
        appendHtml(document, question, true, null, filledText);

    }
    /**
     * Parses an HTML string (from {@link model.Question#getText()} or {@code htmlContent})
     * and appends its formatted content to the Word document. This method uses
     * {@link #processNode(Node, XWPFParagraph, XWPFDocument, Question, boolean, List, boolean, boolean, boolean, boolean, String, String, String)}
     * to recursively handle HTML elements and their styling, with specific logic
     * for MCQ options.
     *
     * @param document The {@link org.apache.poi.xwpf.usermodel.XWPFDocument} to which the HTML content is appended.
     * @param question The {@link model.Question} object from which HTML content is taken.
     * @param withSolutions {@code true} if solutions are to be displayed (influences MCQ checkbox appearance).
     * @param correctOptions A list of correct MCQ option letters, used when {@code withSolutions} is true.
     * @param htmlContent An optional HTML string to use instead of {@code question.getText()}.
     */
    private static void appendHtml(XWPFDocument document, Question question, boolean withSolutions, List<String> correctOptions, String htmlContent) {
        String contentToParse = (htmlContent != null && !htmlContent.isEmpty()) ? htmlContent : question.getText();
        // For Lückentext in student exams, we now preserve the underscores as typed by the teacher.
        // The fixed placeholder '___' is no longer enforced or replaced with a fixed-length line.
        Document parsedHtml = Jsoup.parse(contentToParse);
        // Start with a new paragraph for the HTML content
        XWPFParagraph paragraph = document.createParagraph();
        if ("Lückentext".equals(question.getType())) {
            paragraph.setAlignment(ParagraphAlignment.BOTH);
        }
        processNode(parsedHtml.body(), paragraph, document, question, withSolutions, correctOptions, false, false, false, false, null, null, null);

        // For MCQs, processNode creates new paragraphs for each option, leaving this one empty.
        // This empty paragraph causes a large vertical gap, so we remove it.
        if ("MCQ".equals(question.getType()) && paragraph.getRuns().isEmpty()) {
            int pos = document.getPosOfParagraph(paragraph);
            if (pos != -1) {
                document.removeBodyElement(pos);
            }
        }
    }

    /**
     * Appends styled text to a given {@link org.apache.poi.xwpf.usermodel.XWPFParagraph}.
     * It creates a new {@link org.apache.poi.xwpf.usermodel.XWPFRun} and applies
     * the specified formatting (bold, italic, underline, strikethrough, color, font family).
     *
     * @param paragraph The {@link org.apache.poi.xwpf.usermodel.XWPFParagraph} to which text is appended.
     * @param text The string content to append.
     * @param bold {@code true} for bold text.
     * @param italic {@code true} for italic text.
     * @param underline {@code true} for underlined text.
     * @param strikethrough {@code true} for strikethrough text.
     * @param color The hex color string (e.g., "FF0000" for red), or {@code null} for default.
     * @param fontFamily The font family name (e.g., "Courier New"), or {@code null} for default.
     */
    private static void appendStyledText(XWPFParagraph paragraph, String text, boolean bold, boolean italic, boolean strikethrough, boolean underline, String color, String fontFamily) {
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setItalic(italic);
        run.setUnderline(underline ? org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE : org.apache.poi.xwpf.usermodel.UnderlinePatterns.NONE);
        run.setStrikeThrough(strikethrough);
        if (color != null) {
            run.setColor(color);
        }
        if (fontFamily != null) {
            run.setFontFamily(fontFamily);
        }
    }

    /**
     * Recursively traverses an HTML DOM tree (parsed by Jsoup) and converts each
     * HTML node into corresponding Apache POI Word content. It applies styles
     * (bold, italic, underline, color, font), handles various HTML tags
     * (paragraphs, lists, code blocks), and includes specific logic for
     * formatting MCQ options with checkboxes.
     *
     * @param node The current {@link org.jsoup.nodes.Node} being processed.
     * @param paragraph The current {@link org.apache.poi.xwpf.usermodel.XWPFParagraph} to which content is being added.
     * @param document The overall {@link org.apache.poi.xwpf.usermodel.XWPFDocument}.
     * @param question The {@link model.Question} context for type-specific handling.
     * @param withSolutions {@code true} if solutions are included (influences MCQ checkbox appearance).
     * @param correctOptions A list of correct MCQ option letters, used when {@code withSolutions} is true.
     * @param bold Current bold state inherited from parent nodes.
     * @param italic Current italic state inherited from parent nodes.
     * @param underline Current underline state inherited from parent nodes.
     * @param strikethrough Current strikethrough state inherited from parent nodes.
     * @param color Current color inherited from parent nodes.
     * @param listStyle Current list style ("bullet" or "number") inherited from parent nodes.
     * @param fontFamily Current font family inherited from parent nodes.
     * @return The updated {@link org.apache.poi.xwpf.usermodel.XWPFParagraph} after processing the node.
     */
    private static XWPFParagraph processNode(Node node, XWPFParagraph paragraph, XWPFDocument document, Question question, boolean withSolutions, List<String> correctOptions, boolean bold, boolean italic, boolean underline, boolean strikethrough, String color, String listStyle, String fontFamily) {
        if (node instanceof TextNode) {
            String text = ((TextNode) node).text();
            if (!text.trim().isEmpty() || text.equals(" ")) {
                 appendStyledText(paragraph, text, bold, italic, underline, strikethrough, color, fontFamily);
            }
        } else if (node instanceof Element) {
            Element element = (Element) node;
            String tagName = element.tagName().toLowerCase();
            String style = element.attr("style").toLowerCase();

            boolean newBold = bold || tagName.equals("b") || tagName.equals("strong") || style.contains("font-weight: bold");
            boolean newItalic = italic || tagName.equals("i") || tagName.equals("em") || style.contains("font-style: italic");
            boolean newUnderline = underline || tagName.equals("u") || style.contains("text-decoration: underline");
            boolean newStrikethrough = strikethrough || tagName.equals("strike") || style.contains("text-decoration: line-through");
            String newColor = color;
            String newListStyle = listStyle;
            String newFontFamily = fontFamily;

            if (tagName.equals("font") && element.hasAttr("color")) {
                newColor = element.attr("color").replace("#", "");
            }
             if (tagName.equals("code") || tagName.equals("pre")) {
                newFontFamily = "Courier New";
            }

            if (tagName.equals("pre")) {
                String preText = element.wholeText();
                String[] lines = preText.split("\r?\n");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    if (line.isEmpty() && i == lines.length - 1) {
                        continue; // Skip trailing empty line
                    }
                    XWPFParagraph codeParagraph = document.createParagraph();
                    setParagraphShading(codeParagraph, "F0F0F0");
                    XWPFRun codeRun = codeParagraph.createRun();
                    codeRun.setFontFamily("Courier New");
                    codeRun.setText(line);
                }
                return document.createParagraph();
            }
            
            if (tagName.equals("p") || tagName.equals("div") || tagName.equals("ul") || tagName.equals("ol")) {
                if (!paragraph.getRuns().isEmpty() || paragraph.getCTP().getPPr() != null) {
                    paragraph = document.createParagraph();
                    if ("Lückentext".equals(question.getType())) {
                        paragraph.setAlignment(ParagraphAlignment.BOTH);
                    }
                }
            }
            
            if (tagName.equals("ul")) {
                newListStyle = "bullet";
            } else if (tagName.equals("ol")) {
                newListStyle = "number";
            }

            if (tagName.equals("li")) {
                if ("MCQ".equals(question.getType())) {
                    Document innerDoc = Jsoup.parse(element.html());
                    Element body = innerDoc.body();
                    String firstOptionText = body.ownText().trim();
                    if (!firstOptionText.isEmpty()) {
                        XWPFParagraph optionParagraph = document.createParagraph();
                        optionParagraph.setSpacingAfter(0);
                        optionParagraph.setSpacingBefore(0);
                        optionParagraph.setSpacingBetween(1.0);
                        XWPFRun checkboxRun = optionParagraph.createRun();
                        
                        String optionLetter = extractOptionLetter(firstOptionText);
                        boolean isCorrect = withSolutions && correctOptions != null && correctOptions.contains(optionLetter);
                        checkboxRun.setText(isCorrect ? "☑ " : "☐ ");
                        appendStyledText(optionParagraph, firstOptionText, newBold, newItalic, newUnderline, newStrikethrough, newColor, newFontFamily);
                    }
                    for (Element div : body.select("div")) {
                        XWPFParagraph optionParagraph = document.createParagraph();
                        optionParagraph.setSpacingAfter(0);
                        optionParagraph.setSpacingBefore(0);
                        optionParagraph.setSpacingBetween(1.0);
                        XWPFRun checkboxRun = optionParagraph.createRun();
                        String optionText = div.text().trim();
                        String optionLetter = extractOptionLetter(optionText);
                        boolean isCorrect = withSolutions && correctOptions != null && correctOptions.contains(optionLetter);
                        checkboxRun.setText(isCorrect ? "☑ " : "☐ ");
                        appendStyledText(optionParagraph, optionText, newBold, newItalic, newUnderline, newStrikethrough, newColor, newFontFamily);
                    }
                    return paragraph; 
                } else {
                    if (!paragraph.getRuns().isEmpty()) {
                         paragraph = document.createParagraph();
                    }
                    if ("bullet".equals(listStyle)) {
                        paragraph.setNumID(java.math.BigInteger.ONE);
                    } else if ("number".equals(listStyle)) {
                        paragraph.setNumID(java.math.BigInteger.valueOf(2));
                    }
                }
            }

            for (Node childNode : element.childNodes()) {
                paragraph = processNode(childNode, paragraph, document, question, withSolutions, correctOptions, newBold, newItalic, newUnderline, newStrikethrough, newColor, newListStyle, newFontFamily);
            }

            if (tagName.equals("br")) {
                paragraph.createRun().addBreak();
            }
        }
        return paragraph;
    }

    /**
     * Generates the Word document representation for "Richtig/Falsch" (True/False)
     * type questions. It creates a two-column table where the first column contains
     * the question statement and the second column contains checkboxes for "Richtig" and "Falsch".
     * If solutions are enabled, the correct option is marked.
     *
     * @param document The {@link org.apache.poi.xwpf.usermodel.XWPFDocument} to which the content is added.
     * @param question The {@link model.Question} object of type "Richtig/Falsch".
     * @param withSolutions {@code true} to mark the correct solution, {@code false} otherwise.
     */
    private static void handleRichtigFalsch(XWPFDocument document, Question question, boolean withSolutions) {
        XWPFTable table = document.createTable(1, 2);
        table.setWidth("100%");
        table.getCTTbl().getTblPr().unsetTblBorders();

        XWPFTableRow row = table.getRow(0);
        XWPFTableCell cell1 = row.getCell(0);
        setCellAlignment(cell1, ParagraphAlignment.LEFT, STVerticalJc.CENTER);
        cell1.setText(question.getTitle());

        XWPFTableCell cell2 = row.getCell(1);
        setCellAlignment(cell2, ParagraphAlignment.RIGHT, STVerticalJc.CENTER);
        XWPFParagraph checkboxParagraph = cell2.getParagraphs().get(0);
        checkboxParagraph.setAlignment(ParagraphAlignment.RIGHT);

        String musterloesung = question.getMusterloesung();
        boolean isRichtigCorrect = withSolutions && "Richtig".equalsIgnoreCase(musterloesung);
        boolean isFalschCorrect = withSolutions && "Falsch".equalsIgnoreCase(musterloesung);

        XWPFRun richtigRun = checkboxParagraph.createRun();
        richtigRun.setText((isRichtigCorrect ? "☑" : "☐") + " Richtig");

        checkboxParagraph.createRun().addTab();

        XWPFRun falschRun = checkboxParagraph.createRun();
        falschRun.setText((isFalschCorrect ? "☑" : "☐") + " Falsch");
    }

    // Helper method to extract the option letter (e.g., "A", "B") from the option text
    /**
     * Helper method to extract the option letter (e.g., "A", "B") from the beginning of an option text string.
     * This is typically used in MCQ processing to identify the option for solution matching.
     *
     * @param optionText The full text of the MCQ option.
     * @return The extracted option letter (e.g., "A"), or an empty string if no letter is found.
     */
    private static String extractOptionLetter(String optionText) {
        if (optionText == null || optionText.isEmpty()) {
            return "";
        }
        // Regex to find patterns like "A)", "B.", "C " at the beginning of the string
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^\\s*([A-Z])\\s*[). ]").matcher(optionText);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * Applies a background shading color to a given {@link org.apache.poi.xwpf.usermodel.XWPFParagraph}.
     *
     * @param paragraph The {@link org.apache.poi.xwpf.usermodel.XWPFParagraph} to shade.
     * @param rgb The RGB color in hex format (e.g., "F0F0F0").
     */
    private static void setParagraphShading(XWPFParagraph paragraph, String rgb) {
        if (paragraph.getCTP().getPPr() == null) {
            paragraph.getCTP().addNewPPr();
        }
        if (paragraph.getCTP().getPPr().getShd() != null) {
            paragraph.getCTP().getPPr().unsetShd();
        }
        paragraph.getCTP().getPPr().addNewShd();
        paragraph.getCTP().getPPr().getShd().setVal(STShd.CLEAR); // Use CLEAR for solid fill
        paragraph.getCTP().getPPr().getShd().setColor("auto"); // "auto" means the color is determined by the fill attribute
        paragraph.getCTP().getPPr().getShd().setFill(rgb); // Set the RGB color
    }

    /**
     * Sets both the horizontal and vertical alignment for an {@link org.apache.poi.xwpf.usermodel.XWPFTableCell}.
     *
     * @param cell The {@link org.apache.poi.xwpf.usermodel.XWPFTableCell} to align.
     * @param horizontal The horizontal alignment (e.g., {@link org.apache.poi.xwpf.usermodel.ParagraphAlignment#CENTER}).
     * @param vertical The vertical alignment (e.g., {@link org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc#CENTER}).
     */
    private static void setCellAlignment(XWPFTableCell cell, ParagraphAlignment horizontal, STVerticalJc.Enum vertical) {
        cell.getCTTc().addNewTcPr().addNewVAlign().setVal(vertical);
        for (XWPFParagraph p : cell.getParagraphs()) {
            p.setAlignment(horizontal);
        }
    }

    /**
     * Adds an image to an {@link org.apache.poi.xwpf.usermodel.XWPFParagraph},
     * scaling it down if its width exceeds a defined maximum threshold
     * ({@code MAX_WIDTH_POINTS}).
     *
     * @param paragraph The {@link org.apache.poi.xwpf.usermodel.XWPFParagraph} to which the picture is added.
     * @param imageBytes The byte array of the image.
     * @param type The picture type (e.g., {@link org.apache.poi.xwpf.usermodel.XWPFDocument#PICTURE_TYPE_PNG}).
     * @param filename The suggested filename for the image within the document.
     * @throws InvalidFormatException If the image format is invalid.
     * @throws IOException If an I/O error occurs during image processing.
     */
    private static void addScaledPicture(XWPFParagraph paragraph, byte[] imageBytes, int type, String filename) throws InvalidFormatException, IOException {
        final int MAX_WIDTH_POINTS = 400; // Max width in points (1/72 of an inch)

        ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
        BufferedImage bimg = ImageIO.read(bis);
        double width = bimg.getWidth();
        double height = bimg.getHeight();

        double scale = 1.0;
        if (width > MAX_WIDTH_POINTS) {
            scale = MAX_WIDTH_POINTS / width;
        }

        long finalWidthEMU = (long) (width * scale * Units.EMU_PER_POINT);
        long finalHeightEMU = (long) (height * scale * Units.EMU_PER_POINT);

        XWPFRun imageRun = paragraph.createRun();
        imageRun.addPicture(new ByteArrayInputStream(imageBytes), type, filename, (int)finalWidthEMU, (int)finalHeightEMU);
    }
}
