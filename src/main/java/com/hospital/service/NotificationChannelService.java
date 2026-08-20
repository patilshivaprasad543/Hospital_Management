package com.hospital.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificationChannelService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private WhatsAppService whatsAppService;

    @Value("${smartcare.notifications.email-enabled:true}")
    private boolean emailEnabled;

    @Value("${smartcare.notifications.whatsapp-enabled:false}")
    private boolean whatsAppEnabled;

    @Value("${smartcare.app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    public boolean isWhatsAppEnabled() {
        return whatsAppEnabled && whatsAppService.isTwilioConfigured();
    }

    public void sendOtp(String email, String mobile, String otpCode) {
        if (emailEnabled) {
            emailService.sendOtpEmail(email, otpCode);
        }
        if (isWhatsAppEnabled()) {
            whatsAppService.sendOtp(mobile, otpCode);
        }
    }

    public void sendPasswordResetOtp(String email, String mobile, String otpCode) {
        if (emailEnabled) {
            emailService.sendPasswordResetEmail(email, otpCode);
        }
        if (isWhatsAppEnabled()) {
            whatsAppService.sendMessage(mobile, "SmartCare 360 Password Reset",
                    "Your password reset OTP is: " + otpCode + "\nDo not share this code with anyone.");
        }
    }

    public void sendApprovalNotice(String email, String mobile, String fullName, boolean approved) {
        if (emailEnabled) {
            emailService.sendApprovalEmail(email, fullName, approved);
        }
        if (isWhatsAppEnabled()) {
            String message = approved
                    ? "Your SmartCare 360 account has been approved. You can now log in."
                    : "Your SmartCare 360 account application was not approved. Contact the administrator.";
            whatsAppService.sendMessage(mobile, "Account Update", message);
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
