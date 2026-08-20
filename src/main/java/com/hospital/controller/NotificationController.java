package com.hospital.controller;

import com.hospital.model.Notification;
import com.hospital.model.User;
import com.hospital.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/notifications")
    public String viewNotifications(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        List<Notification> notifications = notificationService.getNotificationsForUser(loggedInUser);
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(loggedInUser));
        model.addAttribute("loggedInUser", loggedInUser);
        return "notifications/list";
    }

    @PostMapping("/notifications/{id}/read")
    @ResponseBody
    public String markNotificationRead(@PathVariable("id") Long id, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "UNAUTHORIZED";
        }
        return notificationService.markAsRead(id, loggedInUser) ? "OK" : "FORBIDDEN";
    }
}
