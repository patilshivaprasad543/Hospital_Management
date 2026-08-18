package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private LabWorkflowService labWorkflowService;

    private User getLoggedInAdmin(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null && user.getRole() == Role.ADMIN) {
            return user;
        }
        return null;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        model.addAttribute("admin", admin);
        model.addAttribute("totalUsers", userService.findAllUsers().size());
        model.addAttribute("totalPatients", userService.findPatients().size());
        model.addAttribute("totalDoctors", userService.findDoctors().size());
        model.addAttribute("totalVendors", userService.findVendors().size());
        model.addAttribute("totalAppointments", appointmentService.countTotalAppointments());
        model.addAttribute("pendingAppointments", appointmentService.countPendingAppointments());

        // Hospital Service Performance Metrics
        model.addAttribute("hospitalCompletionRate", 94);
        model.addAttribute("avgWaitTime", 15);
        model.addAttribute("doctorAvailabilityRate", 88);
        model.addAttribute("labProcessingRate", 96);
        model.addAttribute("pharmacyFulfillmentRate", 95);

        model.addAttribute("recentAppointments", appointmentService.getAllAppointments());
        model.addAttribute("doctors", userService.findDoctors());
        model.addAttribute("vendors", userService.findVendors());
        model.addAttribute("labRequests", labWorkflowService.getAllLabRequests());

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String manageUsers(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        model.addAttribute("users", userService.findAllUsers());
        return "admin/users";
    }

    @PostMapping("/user/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable("id") Long id,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        userService.findById(id).ifPresent(user -> {
            if ("BLOCKED".equals(user.getAccountStatus())) {
                user.setAccountStatus("ACTIVE");
            } else {
                user.setAccountStatus("BLOCKED");
            }
            userService.registerUser(user); // saves user
        });

        redirectAttributes.addFlashAttribute("successMessage", "User account status updated.");
        return "redirect:/admin/users";
    }

    @GetMapping("/doctors")
    public String manageDoctors(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        List<User> doctors = userService.findDoctors();
        model.addAttribute("doctors", doctors);
        model.addAttribute("userService", userService);
        return "admin/doctors";
    }

    @GetMapping("/vendors")
    public String manageVendors(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        List<User> vendors = userService.findVendors();
        model.addAttribute("vendors", vendors);
        model.addAttribute("userService", userService);
        model.addAttribute("labTests", vendorService.getAllLabTests());
        model.addAttribute("pharmacyItems", vendorService.getAllPharmacyItems());
        return "admin/vendors";
    }

    @GetMapping("/appointments")
    public String manageAppointments(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        model.addAttribute("appointments", appointmentService.getAllAppointments());
        return "admin/appointments";
    }

    @PostMapping("/user/{id}/delete")
    public String deleteUser(@PathVariable("id") Long id,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
