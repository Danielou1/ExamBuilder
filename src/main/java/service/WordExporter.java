package service;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

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
        instructionTableRun.setText("\nBitte lesen Sie die folgenden Hinweise aufmerksam durch!");
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

        List<String> correctOptions = null;
        if (withSolutions && "MCQ".equals(question.getType()) && question.getMusterloesung() != null && !question.getMusterloesung().isEmpty()) {
            correctOptions = Arrays.stream(question.getMusterloesung().toUpperCase().split("[,\\s]+"))
                                 .map(String::trim)
                                 .filter(s -> !s.isEmpty())
                                 .collect(Collectors.toList());
        }

        if ("Lückentext".equals(question.getType()) && withSolutions) {
            handleLueckentextSolution(document, question);
        } else if (question.getText() != null && !question.getText().isEmpty()) {
            appendHtml(document, question, withSolutions, correctOptions);
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
        } else {
            // Only add answer lines for non-MCQ and non-Lückentext questions
            if (!"MCQ".equals(question.getType()) && !"Lückentext".equals(question.getType())) {
                for (int i = 0; i < question.getAnswerLines(); i++) {
                    XWPFParagraph answerLine = document.createParagraph();
                    XWPFRun answerLineRun = answerLine.createRun();
                    answerLineRun.setText("__________________________________________________________________________________");
                }
            }
        }

        if (question.getSubQuestions() != null && !question.getSubQuestions().isEmpty()) {
            for (int i = 0; i < question.getSubQuestions().size(); i++) {
                Question subQuestion = question.getSubQuestions().get(i);
                writeQuestion(document, subQuestion, questionNumber + "." + (char)('a' + i), withSolutions);
            }
        }
    }

    private static void handleLueckentextSolution(XWPFDocument document, Question question) {
        String htmlText = question.getText();
        String musterloesung = question.getMusterloesung();

        if (musterloesung == null || musterloesung.trim().isEmpty()) {
            appendHtml(document, question, false, null); // Show blanks
            XWPFParagraph p = document.createParagraph();
            XWPFRun run = p.createRun();
            run.setText("FEHLER: Für diesen Lückentext wurde keine Musterlösung angegeben.");
            run.setColor("FF0000");
            run.setItalic(true);
            return;
        }

        String[] solutions = musterloesung.split(";");
        String filledText = htmlText;

        for (String solution : solutions) {
            // Replace the first occurrence of ___ with a styled solution
            filledText = filledText.replaceFirst("___", "<font color=\"0000FF\"><b>" + solution.trim() + "</b></font>");
        }

        // Now parse the modified HTML and append it
        Document parsedHtml = Jsoup.parse(filledText);
        XWPFParagraph paragraph = document.createParagraph();
        processNode(parsedHtml.body(), paragraph, document, question, true, null, false, false, false, false, null, null);

    }
    private static void appendHtml(XWPFDocument document, Question question, boolean withSolutions, List<String> correctOptions) {
        Document parsedHtml = Jsoup.parse(question.getText());
        // Start with a new paragraph for the HTML content
        XWPFParagraph paragraph = document.createParagraph();
        processNode(parsedHtml.body(), paragraph, document, question, withSolutions, correctOptions, false, false, false, false, null, null);
    }

    private static void appendStyledText(XWPFParagraph paragraph, String text, boolean bold, boolean italic, boolean underline, boolean strikethrough, String color) {
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setItalic(italic);
        run.setUnderline(underline ? org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE : org.apache.poi.xwpf.usermodel.UnderlinePatterns.NONE);
        run.setStrikeThrough(strikethrough);
        if (color != null) {
            run.setColor(color);
        }
    }

    private static XWPFParagraph processNode(Node node, XWPFParagraph paragraph, XWPFDocument document, Question question, boolean withSolutions, List<String> correctOptions, boolean bold, boolean italic, boolean underline, boolean strikethrough, String color, String listStyle) {
        if (node instanceof TextNode) {
            String text = ((TextNode) node).text();
            if (!text.trim().isEmpty() || text.equals(" ")) {
                 appendStyledText(paragraph, text, bold, italic, underline, strikethrough, color);
            }
        } else if (node instanceof Element) {
            Element element = (Element) node;
            String tagName = element.tagName().toLowerCase();
            String style = element.attr("style").toLowerCase();

            // Determine styles from tags and CSS
            boolean newBold = bold || tagName.equals("b") || tagName.equals("strong") || style.contains("font-weight: bold");
            boolean newItalic = italic || tagName.equals("i") || tagName.equals("em") || style.contains("font-style: italic");
            boolean newUnderline = underline || tagName.equals("u") || tagName.equals("em") || style.contains("text-decoration: underline");
            boolean newStrikethrough = strikethrough || tagName.equals("strike") || style.contains("text-decoration: line-through");
            String newColor = color;
            String newListStyle = listStyle;

            if (tagName.equals("font") && element.hasAttr("color")) {
                newColor = element.attr("color").replace("#", "");
            }

            // Handle block-level elements
            if (tagName.equals("p") || tagName.equals("div") || tagName.equals("ul") || tagName.equals("ol")) {
                if (!paragraph.getRuns().isEmpty() || paragraph.getCTP().getPPr() != null) {
                    paragraph = document.createParagraph();
                }
            }
            
            if (tagName.equals("ul")) {
                newListStyle = "bullet";
            } else if (tagName.equals("ol")) {
                newListStyle = "number";
            }

            if (tagName.equals("li")) {
                // Special handling for MCQ list items due to HTMLEditor's output format
                if ("MCQ".equals(question.getType())) {
                    // Parse the inner HTML of the <li> to find individual options (text + <div>s)
                    Document innerDoc = Jsoup.parse(element.html());
                    Element body = innerDoc.body();

                    // Process the first option (direct text within the body)
                    String firstOptionText = body.ownText().trim();
                    if (!firstOptionText.isEmpty()) {
                        XWPFParagraph optionParagraph = document.createParagraph();
                        XWPFRun checkboxRun = optionParagraph.createRun();
                        
                        String optionLetter = extractOptionLetter(firstOptionText);
                        boolean isCorrect = withSolutions && correctOptions != null && correctOptions.contains(optionLetter);
                        checkboxRun.setText(isCorrect ? "☑ " : "☐ ");

                        appendStyledText(optionParagraph, firstOptionText, newBold, newItalic, newUnderline, newStrikethrough, newColor);
                    }

                    // Process subsequent options in <div> tags
                    for (Element div : body.select("div")) {
                        XWPFParagraph optionParagraph = document.createParagraph();
                        XWPFRun checkboxRun = optionParagraph.createRun();

                        String optionText = div.text().trim();
                        String optionLetter = extractOptionLetter(optionText);
                        boolean isCorrect = withSolutions && correctOptions != null && correctOptions.contains(optionLetter);
                        checkboxRun.setText(isCorrect ? "☑ " : "☐ ");

                        appendStyledText(optionParagraph, optionText, newBold, newItalic, newUnderline, newStrikethrough, newColor);
                    }

                    // Skip further processing of children for this <li> as we've handled them
                    return paragraph; 
                } else {
                    // Original list item handling for non-MCQ lists
                    if (!paragraph.getRuns().isEmpty()) {
                         paragraph = document.createParagraph();
                    }
                    if ("bullet".equals(listStyle)) {
                        paragraph.setNumID(java.math.BigInteger.ONE); // Simple bullet point
                    } else if ("number".equals(listStyle)) {
                        paragraph.setNumID(java.math.BigInteger.valueOf(2)); // Simple numbering
                    }
                }
            }

            // Recursive call for child nodes (only if not an MCQ <li> handled above)
            if (!("MCQ".equals(question.getType()) && tagName.equals("li"))) {
                for (Node childNode : element.childNodes()) {
                    paragraph = processNode(childNode, paragraph, document, question, withSolutions, correctOptions, newBold, newItalic, newUnderline, newStrikethrough, newColor, newListStyle);
                }
            }

            // Handle line breaks
            if (tagName.equals("br")) {
                paragraph.createRun().addBreak();
            }
        }
        return paragraph;
    }

    // Helper method to extract the option letter (e.g., "A", "B") from the option text
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
}
