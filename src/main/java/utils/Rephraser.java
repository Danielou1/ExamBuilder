package utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Utility class for rephrasing text without using AI, by replacing
 * words with synonyms loaded from an external thesaurus file.
 * This is primarily used for generating varied versions of exam questions.
 */
public class Rephraser {

    private static final Map<String, List<String>> thesaurus = new HashMap<>();
    private static boolean isLoaded = false;
    private static final Random random = new Random();

    private static void loadThesaurus() {
        if (isLoaded) {
            return;
        }

        try (InputStream is = Rephraser.class.getResourceAsStream("/openthesaurus.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue; // Ignore comments
                }
                
                List<String> potentialSynonyms = Arrays.asList(line.split(";"));

                // Filter out entries with parentheses AND multi-word entries
                List<String> cleanSynonyms = potentialSynonyms.stream()
                                                     .filter(s -> !s.contains("(") && !s.contains(")") && !s.trim().contains(" "))
                                                     .collect(Collectors.toList());

                if (cleanSynonyms.size() > 1) {
                    for (String word : cleanSynonyms) {
                        List<String> others = new ArrayList<>(cleanSynonyms);
                        others.remove(word);
                        thesaurus.put(word.toLowerCase(), others);
                    }
                }
            }
            isLoaded = true;
            System.out.println("Thesaurus loaded successfully with " + thesaurus.size() + " entries.");
        } catch (Exception e) {
            System.err.println("Failed to load thesaurus file.");
            e.printStackTrace();
        }
    }

    public static String rephrase(String originalText) {
        loadThesaurus();
        if (originalText == null || originalText.trim().isEmpty() || !isLoaded || thesaurus.isEmpty()) {
            return originalText;
        }

        String[] lines = originalText.split("\n");
        StringBuilder rephrasedText = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            rephrasedText.append(rephraseLine(line));
            if (i < lines.length - 1) {
                rephrasedText.append("\n");
            }
        }
        return rephrasedText.toString();
    }

    private static String rephraseLine(String line) {
        // Split on word boundaries, but keep delimiters (space, comma, etc.)
        String[] words = line.split("(?<=\\W)|(?=\\W)");
        List<Integer> replaceableIndices = new ArrayList<>();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            // Only consider words that are purely alphabetic and longer than 3 characters
            if (word.length() > 3 && word.matches("[\\p{L}]+") && thesaurus.containsKey(word.toLowerCase())) {
                replaceableIndices.add(i);
            }
        }

        if (replaceableIndices.isEmpty()) {
            return line;
        }

        Collections.shuffle(replaceableIndices);

        // Replace up to 2 words per line, as requested
        int replacements = Math.min(2, replaceableIndices.size());

        for (int i = 0; i < replacements; i++) {
            int indexToReplace = replaceableIndices.get(i);
            String originalWord = words[indexToReplace];
            List<String> synonyms = thesaurus.get(originalWord.toLowerCase());

            if (synonyms != null && !synonyms.isEmpty()) {
                String synonym = synonyms.get(random.nextInt(synonyms.size()));
                // Preserve case
                if (Character.isUpperCase(originalWord.charAt(0))) {
                    synonym = synonym.substring(0, 1).toUpperCase() + synonym.substring(1);
                }
                words[indexToReplace] = synonym;
            }
        }
        return String.join("", words);
    }
}
