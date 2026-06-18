package com.zehranur.deutschifyapp.model;

import com.google.gson.annotations.SerializedName;

public class AiFeedbackRequest {

    @SerializedName("word")
    private String word;

    @SerializedName("user_answer")
    private String userAnswer;

    @SerializedName("correct_answer")
    private String correctAnswer;

    public AiFeedbackRequest(String word, String userAnswer, String correctAnswer) {
        this.word=word;
        this.userAnswer=userAnswer;
        this.correctAnswer=correctAnswer;
    }
}