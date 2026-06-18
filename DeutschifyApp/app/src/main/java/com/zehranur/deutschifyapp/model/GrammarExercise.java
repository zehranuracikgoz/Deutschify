package com.zehranur.deutschifyapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GrammarExercise {

    @SerializedName("id")
    private int id;

    @SerializedName("topic_id")
    private int topicId;

    @SerializedName("question")
    private String question;

    @SerializedName("exercise_type")
    private String exerciseType;

    @SerializedName("options")
    private List<String> options;

    @SerializedName("display_order")
    private int displayOrder;

    public int getId() {return id; }
    public int getTopicId() {return topicId; }
    public String getQuestion() { return question; }
    public String getExerciseType() { return exerciseType; }
    public List<String> getOptions() { return options; }
    public int getDisplayOrder(){ return displayOrder; }
}