package com.hospital.repository;

import com.hospital.model.NotificationDispatchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationDispatchLogRepository extends JpaRepository<NotificationDispatchLog, Long> {

    List<NotificationDispatchLog> findTop200ByOrderByCreatedAtDesc();

    List<NotificationDispatchLog> findTop20ByChannelAndRecipientIgnoreCaseOrderByCreatedAtDesc(
            String channel, String recipient);
}
