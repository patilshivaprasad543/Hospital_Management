package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.service.LabWorkflowService;
import com.hospital.service.PharmacyWorkflowService;
import com.hospital.service.UserService;
import com.hospital.service.VendorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vendor")
public class VendorController {

    @Autowired
    private UserService userService;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private LabWorkflowService labWorkflowService;

    @Autowired
    private PharmacyWorkflowService pharmacyWorkflowService;

    private User getLoggedInVendor(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null && user.getRole() == Role.VENDOR) {
            return user;
        }
        return null;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) return "redirect:/login";

        VendorProfile profile = userService.getVendorProfile(vendor).orElse(new VendorProfile(vendor));

        model.addAttribute("vendor", vendor);
        model.addAttribute("profile", profile);

        if (vendor.getVendorType() == VendorType.LABORATORY) {
            model.addAttribute("vendorTag", "Laboratory");
            model.addAttribute("labTests", vendorService.getLabTestsByVendor(vendor));
            model.addAttribute("labRequests", labWorkflowService.getVendorLabRequests(vendor));
            return "vendor/lab-dashboard";
        } else if (vendor.getVendorType() == VendorType.PHARMACY) {
            model.addAttribute("vendorTag", "Pharmacy");
            model.addAttribute("pharmacyItems", vendorService.getPharmacyItemsByVendor(vendor));
            model.addAttribute("pharmacyOrders", pharmacyWorkflowService.getVendorOrders(vendor));
            return "vendor/pharmacy-dashboard";
        }

        model.addAttribute("labTests", vendorService.getLabTestsByVendor(vendor));
        model.addAttribute("pharmacyItems", vendorService.getPharmacyItemsByVendor(vendor));
        return "vendor/dashboard";
    }

    @PostMapping("/lab-request/{id}/upload-report")
    public String uploadReport(@PathVariable("id") Long id,
                               @RequestParam("reportResult") String reportResult,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) return "redirect:/login";

        labWorkflowService.uploadReport(id, reportResult);
        redirectAttributes.addFlashAttribute("successMessage", "Diagnostic report uploaded and sent to Patient!");
        return "redirect:/vendor/dashboard";
    }

    @PostMapping("/pharmacy-order/{id}/update-status")
    public String updateOrderStatus(@PathVariable("id") Long id,
                                    @RequestParam("status") PharmacyOrderStatus status,
                                    @RequestParam(value = "trackingNotes", required = false) String trackingNotes,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) return "redirect:/login";

        try {
            pharmacyWorkflowService.updateOrderStatus(id, status, vendor, trackingNotes);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order status updated to " + status.getDisplayName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/vendor/dashboard";
    }

    @PostMapping("/lab-test/add")
    public String addLabTest(@ModelAttribute LabTest labTest,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) return "redirect:/login";

        labTest.setVendor(vendor);
        vendorService.saveLabTest(labTest);
        redirectAttributes.addFlashAttribute("successMessage", "Lab Test added successfully!");
        return "redirect:/vendor/dashboard";
    }

    @PostMapping("/lab-test/{id}/delete")
    public String deleteLabTest(@PathVariable("id") Long id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) return "redirect:/login";

        vendorService.deleteLabTest(id);
        redirectAttributes.addFlashAttribute("successMessage", "Lab Test removed.");
        return "redirect:/vendor/dashboard";
    }

    @PostMapping("/pharmacy-item/add")
    public String addPharmacyItem(@ModelAttribute PharmacyItem item,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) return "redirect:/login";

        item.setVendor(vendor);
        vendorService.savePharmacyItem(item);
        redirectAttributes.addFlashAttribute("successMessage", "Pharmacy item added successfully!");
        return "redirect:/vendor/dashboard";
    }

    @PostMapping("/pharmacy-item/{id}/delete")
    public String deletePharmacyItem(@PathVariable("id") Long id,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) return "redirect:/login";

        vendorService.deletePharmacyItem(id);
        redirectAttributes.addFlashAttribute("successMessage", "Pharmacy item removed.");
        return "redirect:/vendor/dashboard";
    }

    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) return "redirect:/login";

        VendorProfile profile = userService.getVendorProfile(vendor).orElse(new VendorProfile(vendor));
        model.addAttribute("vendor", vendor);
        model.addAttribute("profile", profile);
        model.addAttribute("vendorTypes", VendorType.values());
        return "vendor/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute VendorProfile profile,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) return "redirect:/login";

        userService.updateVendorProfile(vendor.getId(), profile);
        redirectAttributes.addFlashAttribute("successMessage", "Vendor business profile updated!");
        return "redirect:/vendor/profile";
    }
}
