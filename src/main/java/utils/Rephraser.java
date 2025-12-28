package utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for rephrasing text without using AI. It replaces words
 * with synonyms from a thesaurus, prioritizing longer words and avoiding
 * common English loanwords to better preserve sentence meaning.
 */
public class Rephraser {

    private static final Map<String, List<String>> thesaurus = new HashMap<>();
    private static final Set<String> englishBlocklist = new HashSet<>();
    private static boolean isLoaded = false;
    private static final Random random = new Random();

    // A simple record to hold information about a potential word to be replaced.
    private record WordCandidate(int index, int length) {}

    private static void loadResources() {
        if (isLoaded) {
            return;
        }
        loadThesaurus();
        loadEnglishBlocklist();
        isLoaded = true;
    }

    private static void loadEnglishBlocklist() {
        try (InputStream is = Rephraser.class.getResourceAsStream("/english_words.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#") && !line.trim().isEmpty()) {
                    englishBlocklist.add(line.trim().toLowerCase());
                }
            }
            System.out.println("English blocklist loaded with " + englishBlocklist.size() + " words.");
        } catch (Exception e) {
            System.err.println("Failed to load English blocklist file.");
            // Continue without the blocklist
        }
    }

    private static void loadThesaurus() {
        try (InputStream is = Rephraser.class.getResourceAsStream("/openthesaurus.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue; // Ignore comments
                }
                
                List<String> potentialSynonyms = Arrays.asList(line.split(";"));
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
            System.out.println("Thesaurus loaded with " + thesaurus.size() + " entries.");
        } catch (Exception e) {
            System.err.println("Failed to load thesaurus file.");
            e.printStackTrace();
        }
    }

    public static String rephrase(String originalText) {
        loadResources();
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
        String[] words = line.split("(?<=\\W)|(?=\\W)");
        List<WordCandidate> candidates = new ArrayList<>();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String lowerCaseWord = word.toLowerCase();

            // A word is a candidate if it's alphabetic, long enough, in the thesaurus, and not on the English blocklist.
            if (word.length() > 3 && 
                word.matches("[\\p{L}]+") && 
                thesaurus.containsKey(lowerCaseWord) && 
                !englishBlocklist.contains(lowerCaseWord)) {
                candidates.add(new WordCandidate(i, word.length()));
            }
        }

        if (candidates.isEmpty()) {
            return line;
        }

        // Sort candidates by length, longest first.
        candidates.sort((c1, c2) -> Integer.compare(c2.length(), c1.length()));
        
        int replacements = Math.min(2, candidates.size());

        for (int i = 0; i < replacements; i++) {
            WordCandidate candidate = candidates.get(i);
            int indexToReplace = candidate.index();
            String originalWord = words[indexToReplace];
            List<String> synonyms = thesaurus.get(originalWord.toLowerCase());

            if (synonyms != null && !synonyms.isEmpty()) {
                String synonym = synonyms.get(random.nextInt(synonyms.size()));
                // Preserve case
                if (Character.isUpperCase(originalWord.charAt(0))) {
                    synonym = Character.toUpperCase(synonym.charAt(0)) + synonym.substring(1);
                }
                words[indexToReplace] = synonym;
            }
        }
        return String.join("", words);
    }
}
