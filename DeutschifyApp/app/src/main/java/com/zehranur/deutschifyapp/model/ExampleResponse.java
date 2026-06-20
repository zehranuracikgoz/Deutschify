package com.zehranur.deutschifyapp.model;

import com.google.gson.annotations.SerializedName;

public class ExampleResponse {
    @SerializedName("word")
    private String word;

    @SerializedName("example_sentence")
    private String exampleSentence;

    public String getWord() {
        return word;
    }
    public String getExampleSentence() {
        return exampleSentence;
    }
}