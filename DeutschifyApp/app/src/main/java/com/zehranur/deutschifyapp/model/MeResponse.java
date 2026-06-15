package com.zehranur.deutschifyapp.model;

import com.google.gson.annotations.SerializedName;

public class MeResponse {
    @SerializedName("id") private int id;
    @SerializedName("username") private String username;
    @SerializedName("email") private String email;
    @SerializedName("total_xp") private int totalXp;
    @SerializedName("daily_streak") private int dailyStreak;
    @SerializedName("preferred_daily_goal") private Integer preferredDailyGoal;
    @SerializedName("created_at") private String createdAt;
    @SerializedName("level_name") private String levelName;

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public int getTotalXp() { return totalXp; }
    public int getDailyStreak() { return dailyStreak; }
    public Integer getPreferredDailyGoal() { return preferredDailyGoal; }
    public String getCreatedAt() { return createdAt; }
    public String getLevelName() { return levelName; }
}