package com.zehranur.deutschifyapp.model;

import com.google.gson.annotations.SerializedName;

public class Word {

    @SerializedName("word_id")
    private int wordId;

    @SerializedName("german_word")
    private String germanWord;

    @SerializedName("turkish_meaning")
    private String turkishMeaning;

    @SerializedName("example_sentence_de")
    private String exampleSentenceDe;

    @SerializedName("ease_factor")
    private float easeFactor;

    @SerializedName("interval_days")
    private int intervalDays;

    @SerializedName("repetition_count")
    private int repetitionCount;

    @SerializedName("status")
    private String status;

    public int getWordId() { return wordId; }
    public String getGermanWord() { return germanWord; }
    public String getTurkishMeaning() { return turkishMeaning; }
    public String getExampleSentenceDe() { return exampleSentenceDe; }
    public float getEaseFactor() { return easeFactor; }
    public int getIntervalDays() { return intervalDays; }
    public int getRepetitionCount() { return repetitionCount; }
    public String getStatus() { return status; }
}