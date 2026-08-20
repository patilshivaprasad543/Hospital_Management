package com.hospital.service;

import com.hospital.dto.NotificationLogEntry;
import com.hospital.model.NotificationDispatchLog;
import com.hospital.repository.NotificationDispatchLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NotificationLogService {

    private static final Pattern OTP_PATTERN = Pattern.compile("\\b(\\d{6})\\b");

    @Autowired
    private NotificationDispatchLogRepository repository;

    @Transactional
    public void log(String channel, String recipient, String subject, String body,
                    boolean delivered, String note) {
        repository.save(new NotificationDispatchLog(channel, recipient, subject, body, delivered, note));
    }

    @Transactional(readOnly = true)
    public List<NotificationLogEntry> getRecentEntries() {
        return repository.findTop200ByOrderByCreatedAtDesc().stream()
                .map(NotificationLogEntry::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<String> findLatestOtpForEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        for (NotificationDispatchLog entry : repository.findTop20ByChannelAndRecipientIgnoreCaseOrderByCreatedAtDesc(
                "EMAIL", email.trim())) {
            if (entry.getSubject() != null && entry.getSubject().toLowerCase().contains("otp")) {
                Optional<String> otp = extractOtp(entry.getBody());
                if (otp.isPresent()) {
                    return otp;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractOtp(String body) {
        if (body == null) {
            return Optional.empty();
        }
        Matcher matcher = OTP_PATTERN.matcher(body);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }
}
