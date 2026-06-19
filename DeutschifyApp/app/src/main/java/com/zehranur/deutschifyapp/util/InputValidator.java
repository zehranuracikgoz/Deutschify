package com.zehranur.deutschifyapp.util;

public class InputValidator {

    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        return email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }
    public static boolean isValidQuality(int quality) {
        return quality >= 1 && quality <= 5;
    }
    public static String articleIdToName(Integer id) {
        if (id==null) return "";
        switch (id) {
            case 1: return "der";
            case 2: return "die";
            case 3: return "das";
            default: return "";
        }
    }
}