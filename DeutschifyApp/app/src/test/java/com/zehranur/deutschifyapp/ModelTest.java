package com.zehranur.deutschifyapp;

import com.zehranur.deutschifyapp.model.AiFeedbackRequest;
import com.zehranur.deutschifyapp.model.WordResponse;
import com.zehranur.deutschifyapp.util.InputValidator;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModelTest {

    @Test
    public void test_word_response_german_word() {
        WordResponse word=new WordResponse();
        word.setGermanWord("Haus");
        assertEquals("Haus", word.getGermanWord());
    }
    @Test
    public void test_artikle_id_mapping() {
        assertEquals("der", InputValidator.articleIdToName(1));
        assertEquals("die", InputValidator.articleIdToName(2));
        assertEquals("das", InputValidator.articleIdToName(3));
    }

    @Test
    public void test_ai_feedback_request_fields() {
        AiFeedbackRequest req = new AiFeedbackRequest("Haus", "DER", "DAS");
        assertEquals("Haus", req.getWord());
        assertEquals("DER", req.getUserAnswer());
        assertEquals("DAS", req.getCorrectAnswer());
    }
}