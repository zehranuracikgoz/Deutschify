package com.zehranur.deutschifyapp.model;

import com.google.gson.annotations.SerializedName;

public class AnswerResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("next_review_days")
    private int nextReviewDays;

    @SerializedName("xp_earned")
    private int xpEarned;

    @SerializedName("status")
    private String status;

    public String getMessage() { return message; }
    public int getNextReviewDays() { return nextReviewDays; }
    public int getXpEarned() { return xpEarned; }
    public String getStatus() { return status; }
}