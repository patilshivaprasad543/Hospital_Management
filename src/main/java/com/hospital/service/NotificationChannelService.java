package com.hospital.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificationChannelService {

    @Autowired
    private EmailService emailService;

    @Value("${smartcare.notifications.email-enabled:true}")
    private boolean emailEnabled;

    @Value("${smartcare.app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    public boolean sendOtp(String email, String mobile, String otpCode) {
        boolean delivered = false;
        if (emailEnabled) {
            delivered = emailService.sendOtpEmail(email, otpCode);
        }
        return delivered;
    }

    public boolean sendPasswordResetOtp(String email, String mobile, String otpCode) {
        boolean delivered = false;
        if (emailEnabled) {
            delivered = emailService.sendPasswordResetEmail(email, otpCode);
        }
        return delivered;
    }

    public void sendWelcomeNotice(String email, String mobile, String fullName, String role) {
        String message = "Your SmartCare 360 " + role.toLowerCase()
                + " account has been created. Sign in with this email and the password you registered.";
        if (emailEnabled) {
            emailService.sendWelcomeEmail(email, fullName, message);
        }
    }

    public void sendApprovalNotice(String email, String mobile, String fullName, boolean approved) {
        if (emailEnabled) {
            emailService.sendApprovalEmail(email, fullName, approved);
        }
    }

    public String buildPortalLink(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return appBaseUrl;
        }
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }
        String base = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
        String path = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        return base + path;
    }
}
