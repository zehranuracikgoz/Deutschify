package com.zehranur.deutschifyapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ReviewResponse {

    @SerializedName("words")
    private List<ReviewWord> words;

    @SerializedName("message")
    private String message;

    public List<ReviewWord> getWords() { return words; }
    public String getMessage() { return message; }

    public static class ReviewWord {

        @SerializedName("id")
        private int id;

        @SerializedName("german_word")
        private String germanWord;

        @SerializedName("turkish_meaning")
        private String turkishMeaning;
        @SerializedName("example_sentence_de")
        private String exampleSentenceDe;

        @SerializedName("example_sentence_tr")
        private String exampleSentenceTr;

        @SerializedName("ease_factor")
        private float easeFactor;

        @SerializedName("repetitions")
        private int repetitions;

        public int getId() {return id; }
        public String getGermanWord() { return germanWord; }
        public String getTurkishMeaning() { return turkishMeaning; }
        public String getExampleSentenceDe() { return exampleSentenceDe; }
        public String getExampleSentenceTr() { return exampleSentenceTr; }
        public float getEaseFactor() { return easeFactor; }
        public int getRepetitions() { return repetitions; }
    }
}