package com.hospital.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveContentMaskerTest {

    @Test
    void detectsOtpMessages() {
        assertTrue(SensitiveContentMasker.isOtpRelated("Your OTP", "ignore"));
        assertTrue(SensitiveContentMasker.isOtpRelated("Hello", "Your verification code is ready"));
        assertFalse(SensitiveContentMasker.isOtpRelated("Appointment", "Your visit is confirmed"));
    }

    @Test
    void masksSixDigitCodes() {
        assertEquals("Code ****** expires soon", SensitiveContentMasker.maskBody("Code 123456 expires soon"));
    }

    @Test
    void redactsOtpBodyForAdminDisplay() {
        String display = SensitiveContentMasker.displayBody("OTP", "Your OTP is 654321");
        assertTrue(display.toLowerCase().contains("redacted"));
        assertFalse(display.contains("654321"));
    }
}
