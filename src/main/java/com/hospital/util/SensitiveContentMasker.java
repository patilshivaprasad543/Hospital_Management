package com.hospital.util;

import java.util.regex.Pattern;

public final class SensitiveContentMasker {

    private static final Pattern SIX_DIGIT_OTP = Pattern.compile("\\b\\d{6}\\b");

    private SensitiveContentMasker() {
    }

    public static boolean isOtpRelated(String subject, String body) {
        if (subject != null && subject.toLowerCase().contains("otp")) {
            return true;
        }
        if (body != null && body.toLowerCase().contains("otp")) {
            return true;
        }
        if (body != null && body.toLowerCase().contains("verification code")) {
            return true;
        }
        return body != null && SIX_DIGIT_OTP.matcher(body).find()
                && body.toLowerCase().contains("password reset");
    }

    public static String maskBody(String body) {
        if (body == null) {
            return null;
        }
        return SIX_DIGIT_OTP.matcher(body).replaceAll("******");
    }

    public static String displaySubject(String subject) {
        if (subject != null && subject.toLowerCase().contains("otp")) {
            return "Verification / reset message";
        }
        return subject;
    }

    public static String displayBody(String subject, String body) {
        if (isOtpRelated(subject, body)) {
            return "Message content redacted. OTP codes are delivered only to the user's email.";
        }
        return maskBody(body);
    }
}
