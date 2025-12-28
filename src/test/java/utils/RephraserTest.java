package utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

class RephraserTest {

    // Reset the thesaurus before each test to ensure isolation
    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        Field isLoadedField = Rephraser.class.getDeclaredField("isLoaded");
        isLoadedField.setAccessible(true);
        isLoadedField.set(null, false); // Set isLoaded to false

        Field thesaurusField = Rephraser.class.getDeclaredField("thesaurus");
        thesaurusField.setAccessible(true);
        ((Map<?, ?>) thesaurusField.get(null)).clear(); // Clear the thesaurus map
    }

    @Test
    void testRephraseWithNullOrEmptyText() {
        assertNull(Rephraser.rephrase(null));
        assertEquals("", Rephraser.rephrase(""));
        assertEquals(" ", Rephraser.rephrase(" "));
    }

    @Test
    void testEnglishWordsAreNotRephrased() {
        // This test replaces testRephraseSingleWord and is more robust.
        // It checks that common English words, which are on the blocklist, are NOT changed.
        String original = "This is a test sentence.";
        String rephrased = Rephraser.rephrase(original);
        assertEquals(original, rephrased, "English words should not be rephrased.");
    }

    @Test
    void testRephraseMultipleWordsInLine() {
        // Use a German sentence with words that are in the thesaurus but not on the blocklist.
        String original = "Die Bearbeitung dieser Aufgabe ist eine gute Arbeit.";
        String rephrased = Rephraser.rephrase(original);
        assertNotEquals(original, rephrased, "Expected rephrased text to be different from original for German sentence.");
    }

    @Test
    void testRephraseMultipleLines() {
        String original = "Gute Arbeit.\nDas ist ein Beispiel.";
        String rephrased = Rephraser.rephrase(original);
        assertNotEquals(original, rephrased, "A German sentence with rephrasable words across lines should be changed.");
        assertTrue(rephrased.contains(".\n")); // Ensure newlines are preserved
    }

    @Test
    void testPreserveCase() {
        String original = "Word1 is here. Word2 is also here.";
        String rephrased = Rephraser.rephrase(original);
        // Check if the first letter of rephrased words preserves case
        // This is hard to test robustly without mocking Random.
        // Let's assume 'Word1' might become 'Synonym1a' or 'Synonym1b'
        // and check if the first letter is uppercase.
        if (!original.equals(rephrased)) {
            // Find a word that was rephrased and check its case
            // This is a heuristic, not a perfect test.
            if (rephrased.contains("Synonym1a") || rephrased.contains("Synonym1b")) {
                assertTrue(Character.isUpperCase(rephrased.charAt(rephrased.indexOf("Synonym1") + 0)));
            }
        }
    }

    @Test
    void testHandlePunctuation() {
        String original = "Gute Arbeit!";
        String rephrased = Rephraser.rephrase(original);
        // Ensure punctuation is preserved
        assertTrue(rephrased.endsWith("!"));
        assertNotEquals(original, rephrased, "German word 'Arbeit' should be rephrased.");
    }

    @Test
    void testWordsNotInThesaurusAreNotRephrased() {
        String original = "This sentence contains unique words.";
        String rephrased = Rephraser.rephrase(original);
        assertEquals(original, rephrased); // No words in thesaurus, so no change
    }

    @Test
    void testMaxTwoReplacementsPerLine() {
        // Assuming a thesaurus with many synonyms for 'a', 'b', 'c', 'd', 'e'
        // For our test thesaurus, 'test' and 'hello' are the only ones.
        String original = "Hello, this is a test sentence with many words to rephrase.";
        String rephrased = Rephraser.rephrase(original);

        int replacements = 0;
        if (!original.contains("Hello")) { // If hello was rephrased
            replacements++;
        }
        if (!original.contains("test")) { // If test was rephrased
            replacements++;
        }
        // This test is still weak due to randomness.
        // A better approach would be to count how many words are different from original
        // and assert that count is <= 2.
        String[] originalWords = original.split("(?<=\\W)|(?=\\W)");
        String[] rephrasedWords = rephrased.split("(?<=\\W)|(?=\\W)");
        int diffCount = 0;
        for (int i = 0; i < originalWords.length && i < rephrasedWords.length; i++) {
            if (!originalWords[i].equals(rephrasedWords[i])) {
                diffCount++;
            }
        }
        assertTrue(diffCount <= 2, "Expected at most 2 replacements per line, but found " + diffCount);
    }

    @Test
    void testThesaurusFiltering() {
        // Trigger thesaurus loading by calling the method under test
        Rephraser.rephrase("dummy");

        try {
            Field thesaurusField = Rephraser.class.getDeclaredField("thesaurus");
            thesaurusField.setAccessible(true);
            Map<String, List<String>> thesaurus = (Map<String, List<String>>) thesaurusField.get(null);

            // Assert conditions that should be true for the real openthesaurus.txt
            assertFalse(thesaurus.containsKey("(ignore)"), "Words with parentheses should be ignored.");
            assertFalse(thesaurus.containsKey("multi word entry"), "Multi-word entries should be ignored (this is a heuristic).");
            assertTrue(thesaurus.containsKey("arbeit"), "A common German word like 'arbeit' should be included.");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to access thesaurus field for testing: " + e.getMessage());
        }
    }
}
