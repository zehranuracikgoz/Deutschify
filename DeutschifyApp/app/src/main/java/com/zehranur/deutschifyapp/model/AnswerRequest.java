package com.zehranur.deutschifyapp.model;

import com.google.gson.annotations.SerializedName;

public class AnswerRequest {

    @SerializedName("user_id")
    private int userId;

    @SerializedName("word_id")
    private int wordId;

    @SerializedName("quality")
    private int quality;

    @SerializedName("session_id")
    private Integer sessionId;

    public AnswerRequest(int userId, int wordId, int quality) {
        this.userId = userId;
        this.wordId = wordId;
        this.quality = quality;
    }

    public AnswerRequest(int userId, int wordId, int quality, int sessionId) {
        this.userId = userId;
        this.wordId = wordId;
        this.quality = quality;
        this.sessionId = sessionId;
    }
}