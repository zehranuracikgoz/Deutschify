package com.zehranur.deutschifyapp.model;

import com.google.gson.annotations.SerializedName;

public class GrammarCheckRequest {
    @SerializedName("exercise_id")
    private int exerciseId;

    @SerializedName("answer")
    private String answer;

    @SerializedName("user_id")
    private Integer userId;

    public GrammarCheckRequest(int exerciseId, String answer, Integer userId) {
        this.exerciseId = exerciseId;
        this.answer = answer;
        this.userId = userId;
    }
}