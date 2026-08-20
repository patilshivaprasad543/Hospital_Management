package com.hospital.controller;

import com.hospital.model.ApprovalStatus;
import com.hospital.model.PortalRole;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.model.VendorType;
import com.hospital.service.NotificationChannelService;
import com.hospital.service.UserService;
import com.hospital.service.EmailService;
import com.hospital.service.WhatsAppService;
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

    @Autowired
    private EmailService emailService;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private NotificationChannelService notificationChannelService;

    @GetMapping("/register")
    public String redirectRegisterPortal() {
        return "redirect:/login";
    }

    @GetMapping("/register/{portalRole}")
    public String showRoleRegisterPage(@PathVariable String portalRole, Model model) {
        PortalRole role = PortalRole.fromPath(portalRole);
        if (role == null || !role.canSelfRegister()) {
            return "redirect:/login";
        }

        User user = new User();
        role.applyToUser(user);
        model.addAttribute("user", user);
        model.addAttribute("portalRole", role);
        return "auth/register-role";
    }

    @PostMapping("/register/{portalRole}")
    public String registerUser(@PathVariable String portalRole,
                               @ModelAttribute("user") User user,
                               @RequestParam(value = "selectedVendorType", required = false) VendorType selectedVendorType,
                               @RequestParam(value = "dateOfBirth", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dateOfBirth,
                               @RequestParam(value = "gender", required = false) String gender,
                               @RequestParam(value = "address", required = false) String address,
                               HttpSession session,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        PortalRole role = PortalRole.fromPath(portalRole);
        if (role == null || !role.canSelfRegister()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Registration is not available for this role.");
            return "redirect:/login";
        }

        try {
            role.applyToUser(user);
            if (user.getRole() == Role.VENDOR && selectedVendorType != null) {
                user.setVendorType(selectedVendorType);
            }

            User registeredUser = userService.registerUser(user, dateOfBirth, gender, address);
            session.setAttribute("pendingVerificationUser", registeredUser);
            boolean mailReady = emailService.isSmtpConfigured();
            redirectAttributes.addFlashAttribute("successMessage", mailReady
                    ? "A 6-digit OTP was sent to " + registeredUser.getEmail() + ". Verify to activate your account, then sign in."
                    : "Account saved. Email could not be sent — use Resend OTP after mail is configured, or try Resend now.");
            return "redirect:/verify-otp?userId=" + registeredUser.getId();
        } catch (Exception e) {
            Optional<User> existingUser = userService.findByEmail(user.getEmail());
            if (existingUser.isEmpty() && user.getMobileNumber() != null) {
                existingUser = userService.findByEmailOrMobile(user.getMobileNumber());
            }
            if (existingUser.isPresent() && !existingUser.get().isVerified()) {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
                return "redirect:/verify-otp?userId=" + existingUser.get().getId();
            }
            role.applyToUser(user);
            model.addAttribute("portalRole", role);
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register-role";
        }
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtpPage(@RequestParam("userId") Long userId, Model model) {
        model.addAttribute("userId", userId);
        model.addAttribute("emailReady", emailService.isSmtpConfigured());
        userService.findById(userId).ifPresent(user -> {
            model.addAttribute("userEmail", user.getEmail());
            model.addAttribute("userMobile", maskMobile(user.getMobileNumber()));
            model.addAttribute("userRole", user.getRole());
            if (user.getRole() == Role.VENDOR) {
                model.addAttribute("portalRole", user.getVendorType() == VendorType.PHARMACY
                        ? PortalRole.PHARMACY : PortalRole.VENDOR);
            } else if (user.getRole() == Role.PATIENT) {
                model.addAttribute("portalRole", PortalRole.PATIENT);
            } else if (user.getRole() == Role.DOCTOR) {
                model.addAttribute("portalRole", PortalRole.DOCTOR);
            }
        });
        return "auth/verify-otp";
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) {
            return "registered mobile";
        }
        return "••••" + mobile.substring(mobile.length() - 4);
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
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Email verified. Your account is activated. Please sign in.");
                    return "redirect:" + PortalRole.loginPathForUser(user);
                }
                redirectAttributes.addFlashAttribute("successMessage",
                        "Email verified! Please submit your documents for admin approval.");
                return "redirect:/submit-documents?userId=" + userId;
            }
        }
        userService.findById(userId).ifPresent(user -> {
            model.addAttribute("userEmail", user.getEmail());
            model.addAttribute("userRole", user.getRole());
        });
        model.addAttribute("errorMessage", "Invalid or expired OTP. Please try again or request a new code.");
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
        if (user.getRole() == Role.VENDOR) {
            model.addAttribute("loginPath", user.getVendorType() == VendorType.PHARMACY
                    ? "/login/pharmacy" : "/login/vendor");
        } else if (user.getRole() == Role.DOCTOR) {
            model.addAttribute("loginPath", "/login/doctor");
        }
        return "auth/submit-documents";
    }

    @PostMapping("/submit-documents")
    public String submitDocuments(@RequestParam("userId") Long userId,
                                  @RequestParam("documentInfo") String documentInfo,
                                  RedirectAttributes redirectAttributes) {
        try {
            userService.submitDocuments(userId, documentInfo);
            User user = userService.findById(userId).orElse(null);
            String loginPath = PortalRole.loginPathForUser(user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Documents submitted successfully. Your account is pending admin approval.");
            return "redirect:" + loginPath;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/submit-documents?userId=" + userId;
        }
    }

    @GetMapping("/resend-otp")
    public String resendOtp(@RequestParam("userId") Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.resendOtp(userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "A new verification code was sent to your registered email. Check inbox and spam.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not resend OTP: " + e.getMessage());
        }
        return "redirect:/verify-otp?userId=" + userId;
    }

    @GetMapping("/login")
    public String showLoginPortal(Model model) {
        model.addAttribute("portalRoles", PortalRole.values());
        return "auth/portal";
    }

    @GetMapping("/login/{portalRole}")
    public String showRoleLoginPage(@PathVariable String portalRole, Model model) {
        PortalRole role = PortalRole.fromPath(portalRole);
        if (role == null) {
            return "redirect:/login";
        }
        model.addAttribute("portalRole", role);
        return "auth/login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam("email") String email,
                            @RequestParam("password") String password,
                            @RequestParam("portalRole") String portalRoleParam,
                            HttpSession session,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        PortalRole portalRole = PortalRole.fromPath(portalRoleParam);
        if (portalRole == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid portal role selected.");
            return "redirect:/login";
        }

        Optional<User> userOptional = userService.loginUser(email, password);
        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (!portalRole.matchesUser(user)) {
                model.addAttribute("portalRole", portalRole);
                model.addAttribute("errorMessage",
                        "This account is not registered as " + portalRole.getLabel() + ". Please select the correct role portal.");
                return "auth/login";
            }

            if (!user.isVerified()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Your account is not verified yet. Enter the code sent to your email during registration, "
                                + "or use Resend on the verification page to request a new code.");
                return "redirect:/verify-otp?userId=" + user.getId();
            }

            if (user.getRole() == Role.DOCTOR || user.getRole() == Role.VENDOR) {
                if (user.getApprovalStatus() == ApprovalStatus.PENDING_DOCUMENTS) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Please submit your documents before logging in.");
                    return "redirect:/submit-documents?userId=" + user.getId();
                }
                if (user.getApprovalStatus() == ApprovalStatus.PENDING_ADMIN) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Your account is pending admin approval. You will be notified once approved.");
                    return "redirect:/login/" + portalRoleParam.toLowerCase();
                }
                if (user.getApprovalStatus() == ApprovalStatus.REJECTED) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Your account application was rejected. Contact the administrator.");
                    return "redirect:/login/" + portalRoleParam.toLowerCase();
                }
            }

            if (!user.canLogin()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Your account is not active. Contact the administrator.");
                return "redirect:/login/" + portalRoleParam.toLowerCase();
            }

            session.setAttribute("loggedInUser", user);
            return getRedirectUrlForRole(user.getRole());
        }

        model.addAttribute("portalRole", portalRole);
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
