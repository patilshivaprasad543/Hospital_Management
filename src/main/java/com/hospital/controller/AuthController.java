package com.hospital.controller;

import com.hospital.model.ApprovalStatus;
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
        model.addAttribute("roles", new Role[]{Role.PATIENT, Role.DOCTOR, Role.VENDOR});
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
            if (user.getRole() == Role.ADMIN) {
                throw new RuntimeException("Admin accounts cannot be self-registered.");
            }
            if (user.getRole() == Role.VENDOR && selectedVendorType != null) {
                user.setVendorType(selectedVendorType);
            }
            User registeredUser = userService.registerUser(user);
            session.setAttribute("pendingVerificationUser", registeredUser);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Enter the OTP sent to your email.");
            return "redirect:/verify-otp?userId=" + registeredUser.getId();
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("roles", new Role[]{Role.PATIENT, Role.DOCTOR, Role.VENDOR});
            model.addAttribute("vendorTypes", VendorType.values());
            return "auth/register";
        }
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtpPage(@RequestParam("userId") Long userId, Model model) {
        model.addAttribute("userId", userId);
        userService.findById(userId).ifPresent(user -> {
            model.addAttribute("userEmail", user.getEmail());
            model.addAttribute("userRole", user.getRole());
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
                if (user.getRole() == Role.PATIENT) {
                    session.setAttribute("loggedInUser", user);
                    redirectAttributes.addFlashAttribute("successMessage", "Account activated! Welcome to SmartCare 360.");
                    return getRedirectUrlForRole(user.getRole());
                }
                redirectAttributes.addFlashAttribute("successMessage",
                        "OTP verified! Please submit your documents for admin approval.");
                return "redirect:/submit-documents?userId=" + userId;
            }
        }
        userService.findById(userId).ifPresent(user -> {
            model.addAttribute("userEmail", user.getEmail());
            model.addAttribute("userRole", user.getRole());
        });
        model.addAttribute("errorMessage", "Invalid OTP code. Please try again.");
        model.addAttribute("userId", userId);
        return "auth/verify-otp";
    }

    @GetMapping("/submit-documents")
    public String showSubmitDocumentsPage(@RequestParam("userId") Long userId, Model model) {
        Optional<User> userOptional = userService.findById(userId);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOptional.get();
        if (user.getRole() != Role.DOCTOR && user.getRole() != Role.VENDOR) {
            return "redirect:/login";
        }
        model.addAttribute("userId", userId);
        model.addAttribute("userRole", user.getRole());
        model.addAttribute("userEmail", user.getEmail());
        return "auth/submit-documents";
    }

    @PostMapping("/submit-documents")
    public String submitDocuments(@RequestParam("userId") Long userId,
                                  @RequestParam("documentInfo") String documentInfo,
                                  RedirectAttributes redirectAttributes) {
        try {
            userService.submitDocuments(userId, documentInfo);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Documents submitted successfully. Your account is pending admin approval.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/submit-documents?userId=" + userId;
        }
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

            if (user.getRole() == Role.DOCTOR || user.getRole() == Role.VENDOR) {
                if (user.getApprovalStatus() == ApprovalStatus.PENDING_DOCUMENTS) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Please submit your documents before logging in.");
                    return "redirect:/submit-documents?userId=" + user.getId();
                }
                if (user.getApprovalStatus() == ApprovalStatus.PENDING_ADMIN) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Your account is pending admin approval. You will be notified once approved.");
                    return "redirect:/login";
                }
                if (user.getApprovalStatus() == ApprovalStatus.REJECTED) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Your account application was rejected. Contact the administrator.");
                    return "redirect:/login";
                }
            }

            if (!user.canLogin()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Your account is not active. Contact the administrator.");
                return "redirect:/login";
            }

            session.setAttribute("loggedInUser", user);
            return getRedirectUrlForRole(user.getRole());
        }
        model.addAttribute("errorMessage", "Invalid email or password!");
        return "auth/login";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        try {
            userService.initiatePasswordReset(email);
            redirectAttributes.addFlashAttribute("successMessage", "Password reset OTP sent to your email.");
            redirectAttributes.addFlashAttribute("resetEmail", email);
            return "redirect:/reset-password";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(Model model) {
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam("email") String email,
                                @RequestParam("resetOtp") String resetOtp,
                                @RequestParam("newPassword") String newPassword,
                                RedirectAttributes redirectAttributes) {
        boolean success = userService.resetPassword(email, resetOtp, newPassword);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Password reset successful. Please log in.");
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Invalid reset OTP. Please try again.");
        redirectAttributes.addFlashAttribute("resetEmail", email);
        return "redirect:/reset-password";
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
