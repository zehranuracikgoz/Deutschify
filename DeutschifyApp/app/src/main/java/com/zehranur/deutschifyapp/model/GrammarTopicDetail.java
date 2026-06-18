package com.zehranur.deutschifyapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GrammarTopicDetail {
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

    @SerializedName("exercises")
    private List<GrammarExercise> exercises;

    public int getId() { return id; }
    public String getTitle() {return title; }
    public String getSlug() { return slug; }
    public String getLevel() {return level; }
    public String getExplanation() { return explanation; }
    public List<GrammarExercise> getExercises() { return exercises; }
}