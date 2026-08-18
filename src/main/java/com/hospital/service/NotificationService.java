package com.hospital.service;

import com.hospital.model.Notification;
import com.hospital.model.NotificationCategory;
import com.hospital.model.User;
import com.hospital.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public Notification sendNotification(User recipient, String title, String message, NotificationCategory category, String linkUrl) {
        Notification notification = new Notification(recipient, title, message, category, linkUrl);
        return notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsForUser(User recipient) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient);
    }

    public long getUnreadCount(User recipient) {
        if (recipient == null) return 0;
        return notificationRepository.countByRecipientAndIsReadFalse(recipient);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}
