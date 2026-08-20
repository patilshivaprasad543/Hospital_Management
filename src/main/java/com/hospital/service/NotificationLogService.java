package com.hospital.service;

import com.hospital.dto.NotificationLogEntry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NotificationLogService {

    private static final int MAX_ENTRIES = 200;
    private static final Pattern OTP_PATTERN = Pattern.compile("\\b(\\d{6})\\b");

    private final ConcurrentLinkedDeque<NotificationLogEntry> entries = new ConcurrentLinkedDeque<>();

    public void log(String channel, String recipient, String subject, String body,
                    boolean delivered, String note) {
        entries.addFirst(new NotificationLogEntry(channel, recipient, subject, body, delivered, note));
        while (entries.size() > MAX_ENTRIES) {
            entries.removeLast();
        }
    }

    public List<NotificationLogEntry> getRecentEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public Optional<String> findLatestOtpForEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String normalized = email.trim().toLowerCase();
        for (NotificationLogEntry entry : entries) {
            if (!"EMAIL".equals(entry.getChannel())) {
                continue;
            }
            if (entry.getRecipient() != null
                    && entry.getRecipient().trim().toLowerCase().equals(normalized)
                    && entry.getSubject() != null
                    && entry.getSubject().toLowerCase().contains("otp")) {
                return extractOtp(entry.getBody());
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
