package com.hospital.controller;

import com.hospital.model.User;
import com.hospital.service.UserService;
import com.hospital.service.VendorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @Autowired
    private UserService userService;

    @Autowired
    private VendorService vendorService;

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser != null) {
            return AuthController.getRedirectUrlForRole(loggedInUser.getRole());
        }
        model.addAttribute("doctors", userService.findApprovedDoctors());
        model.addAttribute("labTests", vendorService.getAllLabTests());
        model.addAttribute("pharmacyItems", vendorService.getAllPharmacyItems());
        return "index";
    }
}
