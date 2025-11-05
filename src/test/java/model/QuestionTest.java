package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

class QuestionTest {

    @Test
    void testQuestionConstructorAndGetters() {
        Question question = new Question("Title", "Text", 10, "Type", 5);
        assertNotNull(question.getId());
        assertEquals("Title", question.getTitle());
        assertEquals("Text", question.getText());
        assertEquals(10, question.getPoints());
        assertEquals("Type", question.getType());
        assertEquals(5, question.getAnswerLines());
        assertTrue(question.getSubQuestions().isEmpty());
        assertEquals("", question.getMusterloesung());
        assertNull(question.getImageBase64());
        assertTrue(question.isSelected());
        assertFalse(question.isStartOnNewPage());
    }

    @Test
    void testSetters() {
        Question question = new Question();
        UUID id = UUID.randomUUID();
        question.setId(id);
        question.setTitle("New Title");
        question.setText("New Text");
        question.setPoints(20);
        question.setType("New Type");
        question.setAnswerLines(8);
        question.setMusterloesung("New Solution");
        question.setImageBase64("base64Image");
        question.setMusterloesungImageBase64("base64SolutionImage");
        question.setSelected(true);
        question.setStartOnNewPage(true);

        assertEquals(id, question.getId());
        assertEquals("New Title", question.getTitle());
        assertEquals("New Text", question.getText());
        assertEquals(20, question.getPoints());
        assertEquals("New Type", question.getType());
        assertEquals(8, question.getAnswerLines());
        assertEquals("New Solution", question.getMusterloesung());
        assertEquals("base64Image", question.getImageBase64());
        assertEquals("base64SolutionImage", question.getMusterloesungImageBase64());
        assertTrue(question.isSelected());
        assertTrue(question.isStartOnNewPage());
    }

    @Test
    void testAddSubQuestion() {
        Question mainQuestion = new Question("Main", "Main Text", 0, "Main Type", 0);
        Question subQuestion = new Question("Sub", "Sub Text", 5, "Sub Type", 3);
        mainQuestion.addSubQuestion(subQuestion);

        assertEquals(1, mainQuestion.getSubQuestions().size());
        assertEquals(subQuestion, mainQuestion.getSubQuestions().get(0));
    }

    @Test
    void testGetPointsWithSubQuestions() {
        Question mainQuestion = new Question("Main", "Main Text", 0, "Main Type", 0);
        Question sub1 = new Question("Sub1", "Text1", 5, "Type1", 2);
        Question sub2 = new Question("Sub2", "Text2", 10, "Type2", 4);
        mainQuestion.addSubQuestion(sub1);
        mainQuestion.addSubQuestion(sub2);

        assertEquals(15, mainQuestion.getPoints());
    }

    @Test
    void testGetPointsWithoutSubQuestions() {
        Question question = new Question("Title", "Text", 10, "Type", 5);
        assertEquals(10, question.getPoints());
    }

    @Test
    void testCopyConstructor() {
        Question original = new Question("Original Title", "Original Text", 20, "Original Type", 10);
        original.setMusterloesung("Original Solution");
        original.setImageBase64("originalImage");
        original.setMusterloesungImageBase64("originalSolutionImage");
        original.setSelected(true);
        original.setStartOnNewPage(true);

        Question subQ1 = new Question("Sub1", "Sub1 Text", 5, "Sub1 Type", 2);
        original.addSubQuestion(subQ1);

        Question copy = new Question(original);

        // Check primitive and String fields
        assertEquals(original.getId(), copy.getId()); // ID should be same for comparison
        assertEquals(original.getTitle(), copy.getTitle());
        assertEquals(original.getText(), copy.getText());
        assertEquals(original.getPoints(), copy.getPoints());
        assertEquals(original.getType(), copy.getType());
        assertEquals(original.getAnswerLines(), copy.getAnswerLines());
        assertEquals(original.getMusterloesung(), copy.getMusterloesung());
        assertEquals(original.getImageBase64(), copy.getImageBase64());
        assertEquals(original.getMusterloesungImageBase64(), copy.getMusterloesungImageBase64());
        assertEquals(original.isSelected(), copy.isSelected());
        assertEquals(original.isStartOnNewPage(), copy.isStartOnNewPage());

        // Check sub-questions deep copy
        assertNotSame(original.getSubQuestions(), copy.getSubQuestions()); // List object should be different
        assertEquals(original.getSubQuestions().size(), copy.getSubQuestions().size());
        assertNotSame(original.getSubQuestions().get(0), copy.getSubQuestions().get(0)); // Question object should be different
        assertEquals(original.getSubQuestions().get(0).getTitle(), copy.getSubQuestions().get(0).getTitle()); // Content should be same
    }

    @Test
    void testIdAndEqualsHashCode() {
        Question q1 = new Question("Title1", "Text1", 10, "Type1", 5);
        Question q2 = new Question("Title2", "Text2", 15, "Type2", 8);
        Question q3 = new Question(q1); // Copy of q1, should have same ID

        // Test equals
        assertEquals(q1, q3); // Same ID, so should be equal
        assertNotEquals(q1, q2); // Different IDs, so not equal

        // Test hashCode
        assertEquals(q1.hashCode(), q3.hashCode());
        assertNotEquals(q1.hashCode(), q2.hashCode());

        // Test with null and different class
        assertNotEquals(q1, null);
        assertNotEquals(q1, new Object());
    }

    @Test
    void testBooleanProperties() {
        Question question = new Question();

        // Test selectedProperty
        assertTrue(question.isSelected());
        question.selectedProperty().set(true);
        assertTrue(question.isSelected());
        question.setSelected(false);
        assertFalse(question.isSelected());

        // Test startOnNewPageProperty
        assertFalse(question.isStartOnNewPage());
        question.startOnNewPageProperty().set(true);
        assertTrue(question.isStartOnNewPage());
        question.setStartOnNewPage(false);
        assertFalse(question.isStartOnNewPage());
    }
}
