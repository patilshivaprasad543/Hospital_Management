package com.hospital.controller;

import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.model.VendorType;
import com.hospital.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", Role.values());
        model.addAttribute("vendorTypes", VendorType.values());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user,
                               @RequestParam(value = "selectedVendorType", required = false) VendorType selectedVendorType,
                               HttpSession session,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        try {
            if (user.getRole() == Role.VENDOR && selectedVendorType != null) {
                user.setVendorType(selectedVendorType);
            }
            User registeredUser = userService.registerUser(user);
            session.setAttribute("pendingVerificationUser", registeredUser);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Enter the OTP sent to your email.");
            return "redirect:/verify-otp?userId=" + registeredUser.getId();
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("roles", Role.values());
            model.addAttribute("vendorTypes", VendorType.values());
            return "auth/register";
        }
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtpPage(@RequestParam("userId") Long userId, Model model) {
        model.addAttribute("userId", userId);
        userService.findById(userId).ifPresent(user -> {
            model.addAttribute("userEmail", user.getEmail());
        });
        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam("userId") Long userId,
                            @RequestParam("otp") String otp,
                            HttpSession session,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        boolean isVerified = userService.verifyOtp(userId, otp);
        if (isVerified) {
            Optional<User> userOptional = userService.findById(userId);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                session.setAttribute("loggedInUser", user);
                redirectAttributes.addFlashAttribute("successMessage", "Account verified successfully! Welcome.");
                return getRedirectUrlForRole(user.getRole());
            }
        }
        userService.findById(userId).ifPresent(user -> {
            model.addAttribute("userEmail", user.getEmail());
        });
        model.addAttribute("errorMessage", "Invalid OTP code. Please try again.");
        model.addAttribute("userId", userId);
        return "auth/verify-otp";
    }

    @GetMapping("/resend-otp")
    public String resendOtp(@RequestParam("userId") Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.resendOtp(userId);
            redirectAttributes.addFlashAttribute("successMessage", "A new OTP code has been generated and sent to your email.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not resend OTP: " + e.getMessage());
        }
        return "redirect:/verify-otp?userId=" + userId;
    }

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        return "auth/login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam("email") String email,
                            @RequestParam("password") String password,
                            HttpSession session,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        Optional<User> userOptional = userService.loginUser(email, password);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (!user.isVerified()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Your account is not verified yet. Please complete OTP verification.");
                return "redirect:/verify-otp?userId=" + user.getId();
            }
            session.setAttribute("loggedInUser", user);
            return getRedirectUrlForRole(user.getRole());
        }
        model.addAttribute("errorMessage", "Invalid email or password!");
        return "auth/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("successMessage", "You have been logged out successfully.");
        return "redirect:/login";
    }

    public static String getRedirectUrlForRole(Role role) {
        switch (role) {
            case PATIENT:
                return "redirect:/patient/dashboard";
            case DOCTOR:
                return "redirect:/doctor/dashboard";
            case ADMIN:
                return "redirect:/admin/dashboard";
            case VENDOR:
                return "redirect:/vendor/dashboard";
            default:
                return "redirect:/login";
        }
    }
}
