package com.zehranur.deutschifyapp.model;

import com.google.gson.annotations.SerializedName;

public class WordResponse {

    @SerializedName("id")
    private int id;
    @SerializedName("german_word")
    private String germanWord;

    @SerializedName("turkish_meaning")
    private String turkishMeaning;

    @SerializedName("article_id")
    private Integer articleId;

    public int getId() { return id; }
    public String getGermanWord() {return germanWord; }
    public String getTurkishMeaning() { return turkishMeaning; }
    public Integer getArticleId() { return articleId; }

    public void setGermanWord(String germanWord) { this.germanWord = germanWord; }
    public void setArticleId(Integer articleId) { this.articleId = articleId; }
}