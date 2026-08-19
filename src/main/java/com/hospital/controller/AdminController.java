package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.repository.LabRequestRepository;
import com.hospital.repository.PharmacyItemRepository;
import com.hospital.repository.PharmacyOrderRepository;
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

    private static final int LOW_STOCK_THRESHOLD = 50;

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private LabWorkflowService labWorkflowService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private LabRequestRepository labRequestRepository;

    @Autowired
    private PharmacyItemRepository pharmacyItemRepository;

    @Autowired
    private PharmacyOrderRepository pharmacyOrderRepository;

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

        long totalAppointments = appointmentService.countTotalAppointments();
        long completedAppointments = appointmentService.countCompletedConsultations();
        int completionRate = totalAppointments > 0
                ? (int) Math.round((completedAppointments * 100.0) / totalAppointments)
                : 0;

        long pendingTests = labRequestRepository.countByStatus("REQUESTED")
                + labRequestRepository.countByStatus("PROCESSING");
        long completedTests = labRequestRepository.countByStatus("REPORT_READY");
        int labProcessingRate = (pendingTests + completedTests) > 0
                ? (int) Math.round((completedTests * 100.0) / (pendingTests + completedTests))
                : 0;

        long availableMedicines = pharmacyItemRepository.countByStockQuantityGreaterThan(LOW_STOCK_THRESHOLD);
        long lowStockMedicines = pharmacyItemRepository
                .countByStockQuantityLessThanEqualAndStockQuantityGreaterThan(LOW_STOCK_THRESHOLD, 0);

        double totalRevenue = pharmacyOrderRepository.findAll().stream()
                .filter(o -> "COMPLETED".equals(o.getStatus()))
                .mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0.0)
                .sum();

        long pendingPayments = pharmacyOrderRepository.countByStatus("PLACED")
                + pharmacyOrderRepository.countByStatus("ACCEPTED");

        model.addAttribute("admin", admin);
        model.addAttribute("totalUsers", userService.findAllUsers().size());
        model.addAttribute("totalPatients", userService.findPatients().size());
        model.addAttribute("totalDoctors", userService.findDoctors().size());
        model.addAttribute("totalVendors", userService.findVendors().size());
        model.addAttribute("totalAppointments", totalAppointments);
        model.addAttribute("todayAppointments", appointmentService.countTodayAppointments());
        model.addAttribute("completedConsultations", completedAppointments);
        model.addAttribute("pendingAppointments", appointmentService.countPendingAppointments());
        model.addAttribute("pendingDoctorApprovals", userService.findPendingDoctors().size());
        model.addAttribute("pendingVendorApprovals", userService.findPendingVendors().size());
        model.addAttribute("pendingTests", pendingTests);
        model.addAttribute("completedTests", completedTests);
        model.addAttribute("availableMedicines", availableMedicines);
        model.addAttribute("lowStockMedicines", lowStockMedicines);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("pendingPayments", pendingPayments);
        model.addAttribute("hospitalCompletionRate", completionRate);
        model.addAttribute("avgWaitTime", 15);
        model.addAttribute("doctorAvailabilityRate", userService.findApprovedDoctors().isEmpty() ? 0 : 88);
        model.addAttribute("labProcessingRate", labProcessingRate);

        model.addAttribute("recentAppointments", appointmentService.getAllAppointments());
        model.addAttribute("pendingDoctors", userService.findPendingDoctors());
        model.addAttribute("pendingVendors", userService.findPendingVendors());
        model.addAttribute("doctors", userService.findDoctors());
        model.addAttribute("vendors", userService.findVendors());
        model.addAttribute("labRequests", labWorkflowService.getAllLabRequests());
        model.addAttribute("recentAuditLogs", auditLogService.getRecentLogs());

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String manageUsers(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        model.addAttribute("users", userService.findPatients());
        return "admin/users";
    }

    @PostMapping("/user/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable("id") Long id,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        userService.findById(id).ifPresent(user -> {
            if (user.getRole() == Role.ADMIN) {
                return;
            }
            if ("BLOCKED".equals(user.getAccountStatus())) {
                user.setAccountStatus("ACTIVE");
            } else {
                user.setAccountStatus("BLOCKED");
            }
            userService.saveUser(user);
            auditLogService.log(admin, "USER_STATUS_TOGGLED", "ADMIN", user.getRole().name(), id,
                    "Status changed to " + user.getAccountStatus());
        });

        redirectAttributes.addFlashAttribute("successMessage", "User account status updated.");
        return "redirect:/admin/users";
    }

    @GetMapping("/doctors")
    public String manageDoctors(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        model.addAttribute("doctors", userService.findDoctors());
        model.addAttribute("pendingDoctors", userService.findPendingDoctors());
        return "admin/doctors";
    }

    @PostMapping("/doctor/{id}/approve")
    public String approveDoctor(@PathVariable("id") Long id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        try {
            userService.approveUser(id, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Doctor approved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/doctors";
    }

    @PostMapping("/doctor/{id}/reject")
    public String rejectDoctor(@PathVariable("id") Long id,
                               @RequestParam(value = "reason", required = false) String reason,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        try {
            userService.rejectUser(id, admin, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Doctor application rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/doctors";
    }

    @GetMapping("/vendors")
    public String manageVendors(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        model.addAttribute("vendors", userService.findVendors());
        model.addAttribute("pendingVendors", userService.findPendingVendors());
        model.addAttribute("labTests", vendorService.getAllLabTests());
        model.addAttribute("pharmacyItems", vendorService.getAllPharmacyItems());
        return "admin/vendors";
    }

    @PostMapping("/vendor/{id}/approve")
    public String approveVendor(@PathVariable("id") Long id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        try {
            userService.approveUser(id, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Vendor approved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/vendors";
    }

    @PostMapping("/vendor/{id}/reject")
    public String rejectVendor(@PathVariable("id") Long id,
                               @RequestParam(value = "reason", required = false) String reason,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        try {
            userService.rejectUser(id, admin, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Vendor application rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/vendors";
    }

    @GetMapping("/appointments")
    public String manageAppointments(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        model.addAttribute("appointments", appointmentService.getAllAppointments());
        return "admin/appointments";
    }

    @GetMapping("/audit-logs")
    public String auditLogs(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        model.addAttribute("auditLogs", auditLogService.getRecentLogs());
        return "admin/audit-logs";
    }

    @PostMapping("/user/{id}/delete")
    public String deleteUser(@PathVariable("id") Long id,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        try {
            userService.findById(id).ifPresent(user -> {
                if (user.getRole() != Role.ADMIN) {
                    auditLogService.log(admin, "USER_DELETED", "ADMIN", user.getRole().name(), id,
                            "Deleted user " + user.getFullName());
                    userService.deleteUser(id);
                }
            });
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
