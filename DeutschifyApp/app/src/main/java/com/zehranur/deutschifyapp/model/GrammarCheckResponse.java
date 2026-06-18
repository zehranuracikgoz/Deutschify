package com.zehranur.deutschifyapp.model;

import com.google.gson.annotations.SerializedName;

public class GrammarCheckResponse {

    @SerializedName("correct")
    private boolean correct;
    @SerializedName("correct_answer")
    private String correctAnswer;

    @SerializedName("explanation")
    private String explanation;

    @SerializedName("xp_earned")
    private int xpEarned;

    public boolean isCorrect() { return correct; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getExplanation() { return explanation; }
    public int getXpEarned() { return xpEarned; }
}