package com.hospital.service;

import com.hospital.model.AuditLog;
import com.hospital.model.User;
import com.hospital.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(User user, String action, String module, String entityType, Long entityId, String details) {
        auditLogRepository.save(new AuditLog(user, action, module, entityType, entityId, details));
    }

    public void log(User user, String action, String module, String details) {
        log(user, action, module, null, null, details);
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }
}
