package com.zehranur.deutschifyapp.model;

import com.google.gson.annotations.SerializedName;

public class AiFeedbackResponse {

    @SerializedName("feedback")
    private String feedback;

    public String getFeedback() { return feedback; }
}