package com.hospital.service;

import com.hospital.model.OtpPurpose;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory OTP store. OTP codes are never persisted to the database.
 */
@Service
public class OtpService {

    private final Map<String, OtpRecord> store = new ConcurrentHashMap<>();

    @Value("${smartcare.otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    public void store(String email, String code, OtpPurpose purpose) {
        if (email == null || email.isBlank() || code == null || code.isBlank()) {
            return;
        }
        store.put(key(email, purpose), new OtpRecord(code.trim(), LocalDateTime.now().plusMinutes(otpExpiryMinutes)));
    }

    public boolean validate(String email, String enteredCode, OtpPurpose purpose) {
        if (email == null || enteredCode == null) {
            return false;
        }
        OtpRecord record = store.get(key(email, purpose));
        if (record == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(record.expiresAt())) {
            store.remove(key(email, purpose));
            return false;
        }
        if (!record.code().equals(enteredCode.trim())) {
            return false;
        }
        store.remove(key(email, purpose));
        return true;
    }

    public void invalidate(String email, OtpPurpose purpose) {
        if (email != null) {
            store.remove(key(email, purpose));
        }
    }

    private String key(String email, OtpPurpose purpose) {
        return email.trim().toLowerCase() + ":" + purpose.name();
    }

    private record OtpRecord(String code, LocalDateTime expiresAt) {
    }
}
