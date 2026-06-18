package com.zehranur.deutschifyapp.model;

import com.google.gson.annotations.SerializedName;

public class GrammarTopic {

    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("slug")
    private String slug;

    @SerializedName("level")
    private String level;

    @SerializedName("explanation")
    private String explanation;

    @SerializedName("display_order")
    private int displayOrder;

    public int getId() { return id; }
    public String getTitle(){ return title; }
    public String getSlug() { return slug; }
    public String getLevel() { return level; }
    public String getExplanation() { return explanation; }
    public int getDisplayOrder() { return displayOrder; }
}