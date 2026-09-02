package com.hospital.controller;

import com.hospital.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ambulance")
public class AmbulanceViewController {

    @GetMapping({"", "/", "/dashboard"})
    public String ambulanceDashboard(HttpSession session, Model model) {
        User loggedInUser = com.hospital.service.UserSessionHelper.getAnyLoggedInUser(session);
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("loggedInUser", loggedInUser);
        return "ambulance/dashboard";
    }
}
