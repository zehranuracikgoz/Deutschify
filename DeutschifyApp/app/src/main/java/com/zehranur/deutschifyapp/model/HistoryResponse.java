package com.zehranur.deutschifyapp.model;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class HistoryResponse {
    @SerializedName("sessions") private List<Session> sessions;

    public List<Session> getSessions() { return sessions; }

    public static class Session {
        @SerializedName("date") private String date;
        @SerializedName("correct") private int correct;
        @SerializedName("wrong") private int wrong;

        public String getDate() { return date; }
        public int getCorrect() { return correct; }
        public int getWrong() { return wrong; }
    }
}
