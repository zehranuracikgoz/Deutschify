package com.zehranur.deutschifyapp.model;

import com.google.gson.annotations.SerializedName;

public class SessionStartResponse {

    @SerializedName("session_id")
    private int sessionId;

    public int getSessionId() {
        return sessionId;
    }
}