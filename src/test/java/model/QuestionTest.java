package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuestionTest {

    @Test
    void testQuestionConstructorAndGetters() {
        Question question = new Question("Title", "Text", 10, "Type", 5);
        assertEquals("Title", question.getTitle());
        assertEquals("Text", question.getText());
        assertEquals(10, question.getPoints());
        assertEquals("Type", question.getType());
        assertEquals(5, question.getAnswerLines());
        assertTrue(question.getSubQuestions().isEmpty());
    }

    @Test
    void testSetters() {
        Question question = new Question();
        question.setTitle("New Title");
        question.setText("New Text");
        question.setPoints(20);
        question.setType("New Type");
        question.setAnswerLines(8);

        assertEquals("New Title", question.getTitle());
        assertEquals("New Text", question.getText());
        assertEquals(20, question.getPoints());
        assertEquals("New Type", question.getType());
        assertEquals(8, question.getAnswerLines());
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
}
