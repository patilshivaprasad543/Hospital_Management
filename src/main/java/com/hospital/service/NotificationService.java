package com.hospital.service;

import com.hospital.model.Notification;
import com.hospital.model.NotificationCategory;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private NotificationChannelService notificationChannelService;

    @Value("${smartcare.notifications.email-enabled:true}")
    private boolean emailEnabled;

    public Notification sendNotification(User recipient, String title, String message,
                                           NotificationCategory category, String linkUrl) {
        Notification notification = notificationRepository.save(
                new Notification(recipient, title, message, category, linkUrl));

        dispatchExternalChannels(recipient, title, message, linkUrl);
        return notification;
    }

    public void notifyBoth(User user1, User user2, String title, String message,
                           NotificationCategory category, String linkUrl1, String linkUrl2) {
        if (user1 != null) {
            sendNotification(user1, title, message, category, linkUrl1);
        }
        if (user2 != null && (user1 == null || !user1.getId().equals(user2.getId()))) {
            sendNotification(user2, title, message, category, linkUrl2);
        }
    }

    private void dispatchExternalChannels(User recipient, String title, String message, String linkUrl) {
        if (recipient == null) {
            return;
        }

        String portalLink = notificationChannelService.buildPortalLink(linkUrl);
        String fullMessage = linkUrl != null
                ? message + "\n\nOpen in portal: " + portalLink
                : message;

        if (emailEnabled && recipient.getEmail() != null && !recipient.getEmail().isBlank()) {
            emailService.sendNotificationEmail(recipient.getEmail(), recipient.getFullName(), title, fullMessage);
        }
        if (notificationChannelService.isWhatsAppEnabled()
                && recipient.getMobileNumber() != null && !recipient.getMobileNumber().isBlank()) {
            whatsAppService.sendMessage(recipient.getMobileNumber(), title, fullMessage);
        }
    }

    public List<Notification> getNotificationsForUser(User recipient) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient);
    }

    public long getUnreadCount(User recipient) {
        if (recipient == null) return 0;
        return notificationRepository.countByRecipientAndIsReadFalse(recipient);
    }

    public boolean markAsRead(Long notificationId, User recipient) {
        return notificationRepository.findById(notificationId)
                .filter(n -> n.getRecipient().getId().equals(recipient.getId()))
                .map(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                    return true;
                })
                .orElse(false);
    }

    public List<Notification> getRecentNotifications() {
        return notificationRepository.findTop100ByOrderByCreatedAtDesc();
    }

    public long getTotalNotificationCount() {
        return notificationRepository.count();
    }

    public long getUnreadNotificationCount() {
        return notificationRepository.countByIsReadFalse();
    }

    public int broadcastNotification(String title, String message, NotificationCategory category,
                                     String audience, String linkUrl, List<User> recipients) {
        if (title == null || title.isBlank() || message == null || message.isBlank()) {
            return 0;
        }

        NotificationCategory resolvedCategory = category != null ? category : NotificationCategory.SYSTEM;
        String resolvedLink = linkUrl != null && !linkUrl.isBlank() ? linkUrl : null;
        int sent = 0;

        for (User recipient : recipients) {
            if (recipient == null || recipient.getRole() == Role.ADMIN) {
                continue;
            }
            if (!matchesAudience(recipient, audience)) {
                continue;
            }
            if (!"ACTIVE".equalsIgnoreCase(recipient.getAccountStatus())) {
                continue;
            }
            sendNotification(recipient, title.trim(), message.trim(), resolvedCategory, resolvedLink);
            sent++;
        }
        return sent;
    }

    private boolean matchesAudience(User user, String audience) {
        if (audience == null || audience.isBlank() || "ALL".equalsIgnoreCase(audience)) {
            return true;
        }
        return switch (audience.toUpperCase()) {
            case "PATIENT" -> user.getRole() == Role.PATIENT;
            case "DOCTOR" -> user.getRole() == Role.DOCTOR;
            case "VENDOR" -> user.getRole() == Role.VENDOR;
            default -> true;
        };
    }
}
