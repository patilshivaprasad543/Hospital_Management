package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.repository.AuditLogRepository;
import com.hospital.repository.ConsultationRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final int LOW_STOCK_THRESHOLD = 50;

    @Autowired private UserService userService;
    @Autowired private AppointmentService appointmentService;
    @Autowired private VendorService vendorService;
    @Autowired private LabWorkflowService labWorkflowService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private LabRequestRepository labRequestRepository;
    @Autowired private PharmacyItemRepository pharmacyItemRepository;
    @Autowired private PharmacyOrderRepository pharmacyOrderRepository;
    @Autowired private DepartmentService departmentService;
    @Autowired private AnnouncementService announcementService;
    @Autowired private BillingService billingService;
    @Autowired private NotificationService notificationService;
    @Autowired private PrescriptionService prescriptionService;
    @Autowired private PharmacyWorkflowService pharmacyWorkflowService;
    @Autowired private HospitalSettingService hospitalSettingService;
    @Autowired private ConsultationRepository consultationRepository;
    @Autowired private AuditLogRepository auditLogRepository;

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

        List<Appointment> appointments = appointmentService.getAllAppointments();
        List<Appointment> recent = appointments.size() > 8 ? appointments.subList(0, 8) : appointments;

        model.addAttribute("admin", admin);
        model.addAttribute("hospitalName", hospitalSettingService.get(HospitalSettingService.HOSPITAL_NAME));
        model.addAttribute("totalUsers", userService.findAllUsers().size());
        model.addAttribute("totalPatients", userService.findPatients().size());
        model.addAttribute("totalDoctors", userService.findDoctors().size());
        model.addAttribute("totalVendors", userService.findVendors().size());
        model.addAttribute("pharmacyCount", userService.findVendorsByType(VendorType.PHARMACY).size());
        model.addAttribute("labCount", userService.findVendorsByType(VendorType.LABORATORY).size());
        model.addAttribute("totalAppointments", totalAppointments);
        model.addAttribute("todayAppointments", appointmentService.countTodayAppointments());
        model.addAttribute("completedConsultations", completedAppointments);
        model.addAttribute("pendingAppointments", appointmentService.countPendingAppointments());
        model.addAttribute("pendingDoctorApprovals", userService.findPendingDoctors().size());
        model.addAttribute("pendingVendorApprovals", userService.findPendingVendors().size());
        model.addAttribute("pendingPharmacyApprovals", userService.findPendingVendorsByType(VendorType.PHARMACY).size());
        model.addAttribute("pendingLabApprovals", userService.findPendingVendorsByType(VendorType.LABORATORY).size());
        model.addAttribute("pendingTests", pendingTests);
        model.addAttribute("completedTests", completedTests);
        model.addAttribute("availableMedicines", availableMedicines);
        model.addAttribute("lowStockMedicines", lowStockMedicines);
        model.addAttribute("prescriptionCount", prescriptionService.countPrescriptions());
        model.addAttribute("pharmacyOrderCount", pharmacyOrderRepository.count());
        model.addAttribute("consultationCount", consultationRepository.count());
        model.addAttribute("totalRevenue", billingService.getTotalRevenue());
        model.addAttribute("pendingPayments", billingService.countPendingPayments());
        model.addAttribute("hospitalCompletionRate", completionRate);
        model.addAttribute("avgWaitTime", 15);
        model.addAttribute("doctorAvailabilityRate", userService.findApprovedDoctors().isEmpty() ? 0 : 88);
        model.addAttribute("labProcessingRate", labProcessingRate);
        model.addAttribute("recentAppointments", recent);
        model.addAttribute("pendingDoctors", userService.findPendingDoctors());
        model.addAttribute("pendingVendors", userService.findPendingVendors());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String manageUsers(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";
        List<User> patients = userService.findPatients();
        Map<Long, PatientProfile> profiles = new HashMap<>();
        for (User patient : patients) {
            userService.getPatientProfile(patient).ifPresent(p -> profiles.put(patient.getId(), p));
        }
        model.addAttribute("users", patients);
        model.addAttribute("profiles", profiles);
        return "admin/users";
    }

    @PostMapping("/user/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable("id") Long id,
                                   @RequestParam(value = "returnTo", required = false) String returnTo,
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

        redirectAttributes.addFlashAttribute("successMessage", "Account status updated.");
        return "redirect:" + safeAdminRedirect(returnTo, "/admin/users");
    }

    @GetMapping("/doctors")
    public String manageDoctors(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";
        List<User> doctors = userService.findDoctors();
        List<User> pending = userService.findPendingDoctors();
        model.addAttribute("doctors", doctors);
        model.addAttribute("pendingDoctors", pending);
        model.addAttribute("doctorProfiles", profileMapForDoctors(doctors));
        model.addAttribute("pendingProfiles", profileMapForDoctors(pending));
        return "admin/doctors";
    }

    @PostMapping("/doctor/{id}/approve")
    public String approveDoctor(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
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
        addVendorModel(model, userService.findVendors(), userService.findPendingVendors(),
                vendorService.getAllLabTests(), vendorService.getAllPharmacyItems());
        return "admin/vendors";
    }

    @GetMapping("/pharmacy")
    public String managePharmacy(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";
        addVendorModel(model,
                userService.findVendorsByType(VendorType.PHARMACY),
                userService.findPendingVendorsByType(VendorType.PHARMACY),
                List.of(),
                vendorService.getAllPharmacyItems());
        model.addAttribute("pageFocus", "PHARMACY");
        return "admin/pharmacy";
    }

    @GetMapping("/laboratories")
    public String manageLaboratories(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";
        addVendorModel(model,
                userService.findVendorsByType(VendorType.LABORATORY),
                userService.findPendingVendorsByType(VendorType.LABORATORY),
                vendorService.getAllLabTests(),
                List.of());
        model.addAttribute("labRequests", labWorkflowService.getAllLabRequests());
        model.addAttribute("pageFocus", "LABORATORY");
        return "admin/laboratories";
    }

    @PostMapping("/vendor/{id}/approve")
    public String approveVendor(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";
        try {
            User vendor = userService.findById(id).orElseThrow();
            userService.approveUser(id, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Vendor approved successfully.");
            return "redirect:" + vendorLanding(vendor);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/vendors";
        }
    }

    @PostMapping("/vendor/{id}/reject")
    public String rejectVendor(@PathVariable("id") Long id,
                               @RequestParam(value = "reason", required = false) String reason,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";
        User vendor = userService.findById(id).orElse(null);
        try {
            userService.rejectUser(id, admin, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Vendor application rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + (vendor != null ? vendorLanding(vendor) : "/admin/vendors");
    }

    @GetMapping("/appointments")
    public String manageAppointments(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";
        model.addAttribute("appointments", appointmentService.getAllAppointments());
        return "admin/appointments";
    }

    @GetMapping("/prescriptions")
    public String monitorPrescriptions(HttpSession session, Model model) {
        if (getLoggedInAdmin(session) == null) return "redirect:/login";
        model.addAttribute("prescriptions", prescriptionService.getAllPrescriptions());
        return "admin/prescriptions";
    }

    @GetMapping("/pharmacy-orders")
    public String monitorPharmacyOrders(HttpSession session, Model model) {
        if (getLoggedInAdmin(session) == null) return "redirect:/login";
        model.addAttribute("pharmacyOrders", pharmacyWorkflowService.getAllOrders());
        return "admin/pharmacy-orders";
    }

    @GetMapping("/lab-reports")
    public String monitorLabReports(HttpSession session, Model model) {
        if (getLoggedInAdmin(session) == null) return "redirect:/login";
        model.addAttribute("labRequests", labWorkflowService.getAllLabRequests());
        return "admin/lab-reports";
    }

    @GetMapping("/medicines")
    public String monitorMedicines(HttpSession session, Model model) {
        if (getLoggedInAdmin(session) == null) return "redirect:/login";
        List<PharmacyItem> items = vendorService.getAllPharmacyItems();
        model.addAttribute("pharmacyItems", items);
        model.addAttribute("lowStockItems", items.stream().filter(PharmacyItem::isLowStock).toList());
        model.addAttribute("expiringItems", items.stream().filter(i -> i.isExpired() || i.isNearExpiry()).toList());
        return "admin/medicines";
    }

    @GetMapping("/billing")
    public String manageBilling(HttpSession session, Model model) {
        if (getLoggedInAdmin(session) == null) return "redirect:/login";
        model.addAttribute("invoices", billingService.getAllInvoices());
        model.addAttribute("payments", billingService.getAllPayments());
        model.addAttribute("totalRevenue", billingService.getTotalRevenue());
        model.addAttribute("pendingPayments", billingService.countPendingPayments());
        return "admin/billing";
    }

    @PostMapping("/billing/{id}/record-payment")
    public String recordPayment(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";
        try {
            billingService.recordPaymentAsAdmin(id, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Payment recorded.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/billing";
    }

    @GetMapping("/records")
    public String medicalRecords(HttpSession session, Model model) {
        if (getLoggedInAdmin(session) == null) return "redirect:/login";
        model.addAttribute("consultations", consultationRepository.findAllDetailed());
        return "admin/records";
    }

    @GetMapping("/audit-logs")
    public String auditLogs(HttpSession session, Model model) {
        if (getLoggedInAdmin(session) == null) return "redirect:/login/admin";
        model.addAttribute("auditLogs", auditLogRepository.findTop100ByOrderByCreatedAtDesc());
        return "admin/audit-logs";
    }

    @PostMapping("/user/{id}/delete")
    public String deleteUser(@PathVariable("id") Long id,
                             @RequestParam(value = "returnTo", required = false) String returnTo,
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
        return "redirect:" + safeAdminRedirect(returnTo, "/admin/users");
    }

    @GetMapping("/departments")
    public String departments(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login/admin";
        model.addAttribute("departments", departmentService.getAllDepartments());
        return "admin/departments";
    }

    @PostMapping("/departments")
    public String addDepartment(@RequestParam String name,
                                @RequestParam(required = false) String description,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login/admin";
        departmentService.save(new Department(name, description));
        auditLogService.log(admin, "DEPARTMENT_CREATED", "ADMIN", name);
        redirectAttributes.addFlashAttribute("successMessage", "Department added.");
        return "redirect:/admin/departments";
    }

    @GetMapping("/announcements")
    public String announcements(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login/admin";
        model.addAttribute("announcements", announcementService.getAll());
        return "admin/announcements";
    }

    @PostMapping("/announcements")
    public String createAnnouncement(@RequestParam String title,
                                     @RequestParam String message,
                                     @RequestParam(defaultValue = "ALL") String audience,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login/admin";
        announcementService.create(title, message, audience, admin);
        redirectAttributes.addFlashAttribute("successMessage", "Announcement published.");
        return "redirect:/admin/announcements";
    }

    @PostMapping("/announcements/{id}/toggle")
    public String toggleAnnouncement(@PathVariable Long id, HttpSession session) {
        if (getLoggedInAdmin(session) == null) return "redirect:/login/admin";
        announcementService.toggleActive(id);
        return "redirect:/admin/announcements";
    }

    @PostMapping("/announcements/{id}/delete")
    public String deleteAnnouncement(@PathVariable Long id, HttpSession session) {
        if (getLoggedInAdmin(session) == null) return "redirect:/login/admin";
        announcementService.delete(id);
        return "redirect:/admin/announcements";
    }

    @GetMapping("/notification-log")
    public String notificationLog(HttpSession session) {
        if (getLoggedInAdmin(session) == null) return "redirect:/login/admin";
        return "redirect:/admin/notifications";
    }

    @GetMapping("/notifications")
    public String notifications(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login/admin";
        model.addAttribute("admin", admin);
        model.addAttribute("categories", NotificationCategory.values());
        model.addAttribute("totalNotifications", notificationService.getTotalNotificationCount());
        model.addAttribute("unreadNotifications", notificationService.getUnreadNotificationCount());
        model.addAttribute("recentNotifications", notificationService.getRecentNotifications());
        return "admin/notifications";
    }

    @PostMapping("/notifications/send")
    public String sendNotification(@RequestParam String title,
                                   @RequestParam String message,
                                   @RequestParam(defaultValue = "SYSTEM") String category,
                                   @RequestParam(defaultValue = "ALL") String audience,
                                   @RequestParam(required = false) String linkUrl,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login/admin";

        NotificationCategory notificationCategory;
        try {
            notificationCategory = NotificationCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException ex) {
            notificationCategory = NotificationCategory.SYSTEM;
        }

        int sent = notificationService.broadcastNotification(
                title, message, notificationCategory, audience, linkUrl, userService.findAllUsers());
        if (sent == 0) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "No notifications were sent. Check the audience and ensure active users exist.");
        } else {
            auditLogService.log(admin, "NOTIFICATION_BROADCAST", "NOTIFICATIONS", null, null,
                    "Sent \"" + title + "\" to " + sent + " user(s) [" + audience + "]");
            redirectAttributes.addFlashAttribute("successMessage",
                    "Portal alert published to " + sent + " user(s).");
        }
        return "redirect:/admin/notifications";
    }

    @GetMapping("/reports")
    public String reports(HttpSession session, Model model) {
        if (getLoggedInAdmin(session) == null) return "redirect:/login/admin";
        List<Appointment> appointments = appointmentService.getAllAppointments();
        Map<String, Long> appointmentsByStatus = new HashMap<>();
        for (AppointmentStatus status : AppointmentStatus.values()) {
            appointmentsByStatus.put(status.getLabel(), appointments.stream().filter(a -> a.getStatus() == status).count());
        }
        List<PharmacyOrder> orders = pharmacyWorkflowService.getAllOrders();
        Map<String, Long> ordersByStatus = new HashMap<>();
        for (PharmacyOrderStatus status : PharmacyOrderStatus.values()) {
            ordersByStatus.put(status.getDisplayName(), orders.stream().filter(o -> o.getStatus() == status).count());
        }
        model.addAttribute("totalPatients", userService.findPatients().size());
        model.addAttribute("totalDoctors", userService.findDoctors().size());
        model.addAttribute("pharmacyCount", userService.findVendorsByType(VendorType.PHARMACY).size());
        model.addAttribute("labCount", userService.findVendorsByType(VendorType.LABORATORY).size());
        model.addAttribute("totalAppointments", appointments.size());
        model.addAttribute("appointmentsByStatus", appointmentsByStatus);
        model.addAttribute("prescriptionCount", prescriptionService.countPrescriptions());
        model.addAttribute("consultationCount", consultationRepository.count());
        model.addAttribute("labRequestCount", labRequestRepository.count());
        model.addAttribute("ordersByStatus", ordersByStatus);
        model.addAttribute("totalRevenue", billingService.getTotalRevenue());
        model.addAttribute("pendingPayments", billingService.countPendingPayments());
        model.addAttribute("lowStockMedicines", pharmacyItemRepository
                .countByStockQuantityLessThanEqualAndStockQuantityGreaterThan(LOW_STOCK_THRESHOLD, 0));
        model.addAttribute("auditLogs", auditLogRepository.findTop100ByOrderByCreatedAtDesc());
        return "admin/reports";
    }

    @GetMapping("/settings")
    public String settings(HttpSession session, Model model) {
        if (getLoggedInAdmin(session) == null) return "redirect:/login/admin";
        model.addAttribute("settings", hospitalSettingService.getAllSettings());
        model.addAttribute("departments", departmentService.getAllDepartments());
        return "admin/settings";
    }

    @GetMapping("/record/{id}")
    public String viewStoredRecord(@PathVariable("id") Long id, HttpSession session, Model model) {
        if (getLoggedInAdmin(session) == null) return "redirect:/login/admin";
        User person = userService.findById(id).orElse(null);
        if (person == null || person.getRole() == Role.ADMIN) {
            return "redirect:/admin/users";
        }
        model.addAttribute("person", person);
        model.addAttribute("patientProfile", userService.getPatientProfile(person).orElse(null));
        model.addAttribute("doctorProfile", userService.getDoctorProfile(person).orElse(null));
        model.addAttribute("vendorProfile", userService.getVendorProfile(person).orElse(null));
        if (person.getRole() == Role.VENDOR && person.getVendorType() == VendorType.PHARMACY) {
            model.addAttribute("pharmacyItems", vendorService.getPharmacyItemsByVendor(person));
        }
        if (person.getRole() == Role.VENDOR && person.getVendorType() == VendorType.LABORATORY) {
            model.addAttribute("labTests", vendorService.getLabTestsByVendor(person));
        }
        return "admin/record";
    }

    @PostMapping("/settings")
    public String saveSettings(@RequestParam String hospitalName,
                               @RequestParam(required = false) String hospitalAddress,
                               @RequestParam(required = false) String hospitalPhone,
                               @RequestParam(required = false) String hospitalEmail,
                               @RequestParam(required = false) String hospitalHours,
                               @RequestParam(value = "emailEnabled", required = false) String emailEnabled,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login/admin";
        hospitalSettingService.saveAll(hospitalName, hospitalAddress, hospitalPhone, hospitalEmail, hospitalHours,
                emailEnabled != null);
        auditLogService.log(admin, "SETTINGS_UPDATED", "ADMIN", "Hospital configuration saved");
        redirectAttributes.addFlashAttribute("successMessage", "System settings saved.");
        return "redirect:/admin/settings";
    }

    private void addVendorModel(Model model, List<User> vendors, List<User> pending,
                                List<LabTest> labTests, List<PharmacyItem> pharmacyItems) {
        Map<Long, VendorProfile> vendorProfiles = new HashMap<>();
        Map<Long, VendorProfile> pendingProfiles = new HashMap<>();
        for (User vendor : vendors) {
            userService.getVendorProfile(vendor).ifPresent(p -> vendorProfiles.put(vendor.getId(), p));
        }
        for (User vendor : pending) {
            userService.getVendorProfile(vendor).ifPresent(p -> pendingProfiles.put(vendor.getId(), p));
        }
        model.addAttribute("vendors", vendors);
        model.addAttribute("pendingVendors", pending);
        model.addAttribute("vendorProfiles", vendorProfiles);
        model.addAttribute("pendingProfiles", pendingProfiles);
        model.addAttribute("labTests", labTests);
        model.addAttribute("pharmacyItems", pharmacyItems);
    }

    private Map<Long, DoctorProfile> profileMapForDoctors(List<User> doctors) {
        Map<Long, DoctorProfile> map = new HashMap<>();
        for (User doctor : doctors) {
            userService.getDoctorProfile(doctor).ifPresent(p -> map.put(doctor.getId(), p));
        }
        return map;
    }

    private String vendorLanding(User vendor) {
        if (vendor.getVendorType() == VendorType.PHARMACY) {
            return "/admin/pharmacy";
        }
        if (vendor.getVendorType() == VendorType.LABORATORY) {
            return "/admin/laboratories";
        }
        return "/admin/vendors";
    }

    private String safeAdminRedirect(String returnTo, String fallback) {
        if (returnTo != null && returnTo.startsWith("/admin/")) {
            return returnTo;
        }
        return fallback;
    }
}
