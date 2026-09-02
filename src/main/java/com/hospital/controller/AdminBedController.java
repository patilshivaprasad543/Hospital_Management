package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.service.AdmissionService;
import com.hospital.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/beds")
public class AdminBedController {

    @Autowired
    private AdmissionService admissionService;

    @Autowired
    private UserService userService;

    private User getLoggedInAdmin(HttpSession session) {
        return com.hospital.service.UserSessionHelper.getLoggedInAdmin(session);
    }

    @GetMapping({"", "/"})
    public String adminBeds(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        model.addAttribute("admin", admin);
        model.addAttribute("totalBedsCount", admissionService.getTotalBedCount());
        model.addAttribute("availableBedsCount", admissionService.getAvailableBedCount());
        model.addAttribute("occupiedBedsCount", admissionService.getOccupiedBedCount());
        model.addAttribute("maintenanceBedsCount", admissionService.getMaintenanceBedCount());
        model.addAttribute("pendingRequestsCount", admissionService.getPendingAdmissionCount());

        model.addAttribute("pendingRequests", admissionService.getPendingAdmissions());
        model.addAttribute("activeAdmissions", admissionService.getActiveAdmissions());
        model.addAttribute("allAdmissions", admissionService.getAllAdmissions());
        model.addAttribute("beds", admissionService.getAllBeds());
        model.addAttribute("availableBeds", admissionService.getAvailableBeds());
        model.addAttribute("wards", admissionService.getAllWards());
        model.addAttribute("rooms", admissionService.getAllRooms());
        model.addAttribute("patients", userService.findPatients());
        model.addAttribute("doctors", userService.findApprovedDoctors());

        return "admin/beds";
    }

    @PostMapping("/allocate")
    public String allocateBed(@RequestParam("admissionId") Long admissionId,
                             @RequestParam("bedId") Long bedId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        try {
            admissionService.allocateBedAndAdmit(admissionId, bedId);
            redirectAttributes.addFlashAttribute("successMessage", "Bed allocated and patient admitted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/beds";
    }

    @PostMapping("/direct-allocate")
    public String directAllocateBed(@RequestParam("patientId") Long patientId,
                                   @RequestParam(value = "doctorId", required = false) Long doctorId,
                                   @RequestParam("bedId") Long bedId,
                                   @RequestParam(value = "reason", required = false, defaultValue = "Direct Inpatient Admission") String reason,
                                   @RequestParam(value = "notes", required = false) String notes,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        try {
            admissionService.directAllocateAndAdmit(patientId, doctorId, bedId, reason, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Patient directly allocated to bed and admitted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/beds";
    }

    @PostMapping("/reject")
    public String rejectRequest(@RequestParam("admissionId") Long admissionId,
                               @RequestParam(value = "reason", required = false) String reason,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        try {
            admissionService.rejectBedBooking(admissionId, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Bed booking request cancelled.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/beds";
    }

    @PostMapping("/discharge")
    public String dischargePatient(@RequestParam("admissionId") Long admissionId,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        try {
            admissionService.dischargePatientSimple(admissionId);
            redirectAttributes.addFlashAttribute("successMessage", "Patient discharged and bed released.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/beds";
    }

    @PostMapping("/update-status")
    public String updateBedStatus(@RequestParam("bedId") Long bedId,
                                  @RequestParam("status") BedStatus status,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        try {
            admissionService.updateBedStatus(bedId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Bed status updated to " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/beds";
    }

    @PostMapping("/add-ward")
    public String addWard(@RequestParam("name") String name,
                          @RequestParam("category") String category,
                          @RequestParam(value = "description", required = false) String description,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        try {
            admissionService.createWard(name, category, description);
            redirectAttributes.addFlashAttribute("successMessage", "New ward '" + name + "' added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/beds";
    }

    @PostMapping("/add-room")
    public String addRoom(@RequestParam("wardId") Long wardId,
                          @RequestParam("roomNumber") String roomNumber,
                          @RequestParam("roomType") String roomType,
                          @RequestParam(value = "dailyRate", defaultValue = "500.0") Double dailyRate,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        try {
            admissionService.createRoom(wardId, roomNumber, roomType, dailyRate);
            redirectAttributes.addFlashAttribute("successMessage", "Room " + roomNumber + " added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/beds";
    }

    @PostMapping("/add-bed")
    public String addBed(@RequestParam("roomId") Long roomId,
                         @RequestParam("bedNumber") String bedNumber,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) return "redirect:/login";

        try {
            admissionService.createBed(roomId, bedNumber);
            redirectAttributes.addFlashAttribute("successMessage", "Bed " + bedNumber + " added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/beds";
    }
}
