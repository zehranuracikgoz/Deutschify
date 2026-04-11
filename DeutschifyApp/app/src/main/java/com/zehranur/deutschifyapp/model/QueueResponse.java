package com.zehranur.deutschifyapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class QueueResponse {

    @SerializedName("queue")
    private List<Word> queue;

    @SerializedName("message")
    private String message;

    public List<Word> getQueue() { return queue; }
    public String getMessage() { return message; }
}