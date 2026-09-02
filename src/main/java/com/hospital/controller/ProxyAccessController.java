package com.hospital.controller;

import com.hospital.model.PatientProxy;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.repository.PatientProxyRepository;
import com.hospital.repository.UserRepository;
import com.hospital.service.UserSessionHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/patient/proxy-access")
public class ProxyAccessController {

    @Autowired
    private PatientProxyRepository patientProxyRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping({"", "/"})
    public String showProxyAccess(HttpSession session, Model model) {
        User patient = UserSessionHelper.getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        List<PatientProxy> proxiesGrown = patientProxyRepository.findByPatientAndStatus(patient, "ACTIVE");
        List<PatientProxy> managingProxies = patientProxyRepository.findByProxyUserAndStatus(patient, "ACTIVE");

        model.addAttribute("patient", patient);
        model.addAttribute("loggedInUser", patient);
        model.addAttribute("myCaregivers", proxiesGrown);
        model.addAttribute("managingDependents", managingProxies);
        return "patient/proxy-access";
    }

    @PostMapping("/add")
    public String addProxyUser(@RequestParam("proxyEmail") String proxyEmail,
                               @RequestParam("relationship") String relationship,
                               @RequestParam(value = "accessLevel", defaultValue = "FULL_ACCESS") String accessLevel,
                               HttpSession session, RedirectAttributes redirectAttributes) {
        User patient = UserSessionHelper.getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        User proxyUser = userRepository.findByEmail(proxyEmail.trim().toLowerCase()).orElse(null);
        if (proxyUser == null || proxyUser.getRole() != Role.PATIENT) {
            redirectAttributes.addFlashAttribute("errorMessage", "Caregiver / Proxy user email not found. Please ensure they have a registered patient account.");
            return "redirect:/patient/proxy-access";
        }

        if (proxyUser.getId().equals(patient.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot grant proxy access to yourself.");
            return "redirect:/patient/proxy-access";
        }

        if (patientProxyRepository.existsByPatientAndProxyUser(patient, proxyUser)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Proxy relationship already exists.");
            return "redirect:/patient/proxy-access";
        }

        PatientProxy proxy = new PatientProxy(patient, proxyUser, relationship, accessLevel);
        patientProxyRepository.save(proxy);

        redirectAttributes.addFlashAttribute("successMessage", "Caregiver proxy access granted to " + proxyUser.getFullName() + " successfully!");
        return "redirect:/patient/proxy-access";
    }

    @PostMapping("/{id}/revoke")
    public String revokeProxy(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User patient = UserSessionHelper.getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        PatientProxy proxy = patientProxyRepository.findById(id).orElse(null);
        if (proxy != null && proxy.getPatient().getId().equals(patient.getId())) {
            proxy.setStatus("REVOKED");
            patientProxyRepository.save(proxy);
            redirectAttributes.addFlashAttribute("successMessage", "Proxy access revoked.");
        }
        return "redirect:/patient/proxy-access";
    }
}
