package com.hospital.service;

import com.hospital.dto.NotificationLogEntry;
import com.hospital.model.NotificationDispatchLog;
import com.hospital.repository.NotificationDispatchLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationLogService {

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
}
