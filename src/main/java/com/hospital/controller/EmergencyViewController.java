package com.hospital.controller;

import com.hospital.model.User;
import com.hospital.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/emergency")
public class EmergencyViewController {

    @Autowired
    private UserService userService;

    @GetMapping({"", "/", "/dashboard"})
    public String emergencyDashboard(HttpSession session, Model model) {
        User loggedInUser = com.hospital.service.UserSessionHelper.getAnyLoggedInUser(session);
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("doctors", userService.findApprovedDoctors());
        return "emergency/emergency-dashboard";
    }
}
