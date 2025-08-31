package utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RephraserTest {

    @Test
    void testRephraseWithNullOrEmptyText() {
        assertNull(Rephraser.rephrase(null));
        assertEquals("", Rephraser.rephrase(""));
        assertEquals(" ", Rephraser.rephrase(" "));
    }
}
