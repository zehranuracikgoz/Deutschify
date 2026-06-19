package com.zehranur.deutschifyapp.model;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class HistoryResponse {
    @SerializedName("sessions") private List<Session> sessions ;

    public List<Session> getSessions() { return sessions;}

    public static class Session {
        @SerializedName("date")         private String  date;
        @SerializedName("time")         private String  time;
        @SerializedName("module_type")  private String  moduleType;
        @SerializedName("correct")      private int     correct;
        @SerializedName("wrong")        private int     wrong;
        @SerializedName("xp_earned")    private int     xpEarned;
        @SerializedName("success_rate") private Integer successRate;

        public String  getDate()        { return date; }
        public String  getTime()        { return time; }
        public String  getModuleType()  { return moduleType != null ? moduleType : "flashcard"; }
        public int     getCorrect()     { return correct;}
        public int     getWrong()       { return wrong; }
        public int     getXpEarned()    { return xpEarned; }
        public Integer getSuccessRate() { return successRate; }
    }
}