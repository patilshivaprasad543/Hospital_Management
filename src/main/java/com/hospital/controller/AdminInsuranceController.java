package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.service.InsuranceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/insurance")
public class AdminInsuranceController {

    @Autowired
    private InsuranceService insuranceService;

    private User getLoggedInAdmin(HttpSession session) {
        return com.hospital.service.UserSessionHelper.getLoggedInAdmin(session);
    }

    @GetMapping({"", "/", "/claims"})
    public String adminInsurance(@RequestParam(value = "status", required = false) String status,
                                 HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        model.addAttribute("insurances", insuranceService.getAllInsurances());
        model.addAttribute("claims", insuranceService.getClaimsFiltered(status));
        model.addAttribute("selectedStatus", status != null ? status.toUpperCase() : "ALL");
        model.addAttribute("pendingClaimsCount", insuranceService.countPendingClaims());
        model.addAttribute("approvedClaimsCount", insuranceService.countApprovedClaims());
        model.addAttribute("totalClaimValue", insuranceService.getTotalClaimValue());
        model.addAttribute("totalPoliciesCount", insuranceService.getAllInsurances().size());
        model.addAttribute("admin", admin);
        model.addAttribute("loggedInUser", admin);
        model.addAttribute("activePage", "insurance");
        return "admin/insurance";
    }

    @PostMapping("/claim/status")
    public String updateClaimStatus(@RequestParam("claimId") Long claimId,
                                    @RequestParam("status") String status,
                                    @RequestParam(value = "remarks", required = false) String remarks,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        try {
            insuranceService.updateClaimStatus(claimId, status, remarks, admin);
            redirectAttributes.addFlashAttribute("successMessage", "✅ Claim #" + claimId + " status updated to " + status + " successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating claim: " + e.getMessage());
        }
        return "redirect:/admin/insurance";
    }
}
