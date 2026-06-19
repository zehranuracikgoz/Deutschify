package com.zehranur.deutschifyapp.util;

public class SrsCalculator {

    public static class Result {
        public final double easeFactor;
        public final int intervalDays;
        public final int repetitionCount;
        public final String status;

        Result(double easeFactor, int intervalDays, int repetitionCount, String status) {
            this.easeFactor = easeFactor;
            this.intervalDays = intervalDays;
            this.repetitionCount = repetitionCount;
            this.status = status;
        }
    }
    public static Result calculateNextReview(double easeFactor, int intervalDays,
                                              int repetitionCount, int quality) {
        double newEf = easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        newEf = Math.max(1.3, newEf);

        int newRepetition;
        int newInterval;

        if (quality < 3) {
            newRepetition = 0;
            newInterval = 1;
        } else {
            newRepetition = repetitionCount + 1;
            if (repetitionCount == 0) {
                newInterval = 1;
            } else if (repetitionCount == 1) {
                newInterval = 6;
            } else {
                newInterval = (int) Math.round(intervalDays * newEf);
            }
        }

        String status=newRepetition < 3 ? "learning" : "review";
        return new Result(newEf, newInterval, newRepetition, status);
    }
}