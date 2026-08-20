package com.hospital.service;

import com.hospital.model.OtpCode;
import com.hospital.model.OtpPurpose;
import com.hospital.repository.OtpCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OtpService {

    @Autowired
    private OtpCodeRepository otpCodeRepository;

    @Value("${smartcare.otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    @Transactional
    public void store(String email, String code, OtpPurpose purpose) {
        if (email == null || email.isBlank() || code == null || code.isBlank() || purpose == null) {
            return;
        }
        String key = key(email);
        otpCodeRepository.deleteByLookupKeyAndPurpose(key, purpose);
        otpCodeRepository.save(new OtpCode(key, code.trim(), purpose, LocalDateTime.now().plusMinutes(otpExpiryMinutes)));
    }

    @Transactional
    public boolean validate(String email, String enteredCode, OtpPurpose purpose) {
        if (email == null || enteredCode == null || purpose == null) {
            return false;
        }
        String key = key(email);
        OtpCode record = otpCodeRepository.findByLookupKeyAndPurpose(key, purpose).orElse(null);
        if (record == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(record.getExpiresAt()) || !record.getCode().equals(enteredCode.trim())) {
            return false;
        }
        otpCodeRepository.deleteByLookupKeyAndPurpose(key, purpose);
        return true;
    }

    @Transactional
    public void invalidate(String email, OtpPurpose purpose) {
        if (email != null && purpose != null) {
            otpCodeRepository.deleteByLookupKeyAndPurpose(key(email), purpose);
        }
    }

    private String key(String email) {
        return email.trim().toLowerCase();
    }
}
