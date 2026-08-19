package com.hospital.controller;

import com.hospital.model.PortalRole;
import com.hospital.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {

    @GetMapping("/")
    public String home(HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser != null) {
            return AuthController.getRedirectUrlForRole(loggedInUser.getRole());
        }
        return "redirect:/login";
    }
}
