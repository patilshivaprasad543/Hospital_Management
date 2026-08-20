package com.hospital.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class WhatsAppService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private NotificationLogService notificationLogService;

    @Value("${smartcare.whatsapp.enabled:false}")
    private boolean enabled;

    @Value("${smartcare.notifications.whatsapp-enabled:true}")
    private boolean whatsAppNotificationsEnabled;

    @Value("${smartcare.whatsapp.account-sid:}")
    private String accountSid;

    @Value("${smartcare.whatsapp.auth-token:}")
    private String authToken;

    @Value("${smartcare.whatsapp.from:whatsapp:+14155238886}")
    private String fromNumber;

    @Async
    public void sendMessage(String mobileNumber, String title, String message) {
        String body = "*" + title + "*\n\n" + message + "\n\n— SmartCare 360";
        logMessage(mobileNumber, body);

        if (!whatsAppNotificationsEnabled) {
            notificationLogService.log("WHATSAPP", mobileNumber, title, body, false, "WhatsApp channel disabled in settings");
            return;
        }

        if (mobileNumber == null || mobileNumber.isBlank()) {
            notificationLogService.log("WHATSAPP", mobileNumber, title, body, false, "Recipient mobile number is blank");
            return;
        }

        if (!isTwilioConfigured()) {
            notificationLogService.log("WHATSAPP", mobileNumber, title, body, false,
                    "Twilio not configured — logged to console and notification log");
            return;
        }

        try {
            String to = formatWhatsAppNumber(mobileNumber);
            if (to.isBlank()) {
                notificationLogService.log("WHATSAPP", mobileNumber, title, body, false, "Invalid mobile number format");
                return;
            }

            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(HttpHeaders.AUTHORIZATION, basicAuth(accountSid, authToken));

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("From", fromNumber);
            form.add("To", to);
            form.add("Body", body);

            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(form, headers), String.class);
            logger.info("WhatsApp message sent to {}", to);
            notificationLogService.log("WHATSAPP", mobileNumber, title, body, true, "Sent via Twilio");
        } catch (Exception e) {
            logger.warn("Could not send WhatsApp to {}: {}", mobileNumber, e.getMessage());
            notificationLogService.log("WHATSAPP", mobileNumber, title, body, false, e.getMessage());
        }
    }

    @Async
    public void sendOtp(String mobileNumber, String otpCode) {
        sendMessage(mobileNumber, "SmartCare 360 OTP",
                "Your verification OTP is: " + otpCode + "\nValid for 10 minutes. Do not share this code.");
    }

    public boolean isTwilioConfigured() {
        return enabled && accountSid != null && !accountSid.isBlank()
                && authToken != null && !authToken.isBlank();
    }

    private void logMessage(String mobileNumber, String body) {
        logger.info("\n=======================================================");
        logger.info("WHATSAPP NOTIFICATION TO: {}", mobileNumber);
        logger.info("BODY:\n{}", body);
        logger.info("=======================================================\n");
    }

    private String formatWhatsAppNumber(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return "";
        }
        String digits = mobile.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) {
            return "whatsapp:" + digits;
        }
        if (digits.length() == 10) {
            return "whatsapp:+91" + digits;
        }
        return "whatsapp:+" + digits;
    }

    private String basicAuth(String user, String pass) {
        String token = user + ":" + pass;
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }
}
