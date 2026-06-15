package com.zehranur.deutschifyapp.model;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StatsResponse {
    @SerializedName("total_xp") private int totalXp;
    @SerializedName("daily_streak") private int dailyStreak;
    @SerializedName("weekly_sessions") private List<Integer> weeklySessions;
    @SerializedName("total_correct") private int totalCorrect;
    @SerializedName("total_wrong") private int totalWrong;
    @SerializedName("success_rate") private int successRate;

    public int getTotalXp() { return totalXp; }
    public int getDailyStreak() { return dailyStreak; }
    public List<Integer> getWeeklySessions() { return weeklySessions; }
    public int getTotalCorrect() { return totalCorrect; }
    public int getTotalWrong() { return totalWrong; }
    public int getSuccessRate() { return successRate; }
}