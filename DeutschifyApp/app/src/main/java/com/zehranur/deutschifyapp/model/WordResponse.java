package com.zehranur.deutschifyapp.model;

import com.google.gson.annotations.SerializedName;

public class WordResponse {

    @SerializedName("id")
    private int id;
    @SerializedName("german_word")
    private String germanWord;

    @SerializedName("turkish_meaning")
    private String turkishMeaning;

    @SerializedName("example_sentence_de")
    private String exampleSentenceDe;

    @SerializedName("example_sentence_tr")
    private String exampleSentenceTr;

    @SerializedName("article_id")
    private Integer articleId;

    public int getId() { return id; }
    public String getGermanWord() { return germanWord; }
    public String getTurkishMeaning() { return turkishMeaning; }
    public String getExampleSentenceDe() { return exampleSentenceDe; }
    public String getExampleSentenceTr() { return exampleSentenceTr; }
    public Integer getArticleId() { return articleId; }

    public void setGermanWord(String germanWord) { this.germanWord = germanWord; }
    public void setArticleId(Integer articleId) { this.articleId = articleId; }
}