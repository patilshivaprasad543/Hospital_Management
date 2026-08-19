package com.hospital.controller;

import com.hospital.dto.CareCaseWorkflow;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.service.CareWorkflowService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/workflow")
public class WorkflowController {

    @Autowired
    private CareWorkflowService careWorkflowService;

    @GetMapping
    public String workflowOverview(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null) {
            return redirectForRole(user.getRole());
        }
        return "workflow/overview";
    }

    @GetMapping("/patient")
    public String patientWorkflow(HttpSession session, Model model) {
        User patient = getUserWithRole(session, Role.PATIENT);
        if (patient == null) return "redirect:/login";

        List<CareCaseWorkflow> workflows = careWorkflowService.getPatientWorkflows(patient);
        model.addAttribute("patient", patient);
        model.addAttribute("workflows", workflows);
        model.addAttribute("activeWorkflows", workflows.stream()
                .filter(w -> w.getCurrentState() != null
                        && !w.getCurrentState().name().equals("COMPLETED")
                        && !w.getCurrentState().name().equals("REJECTED")
                        && !w.getCurrentState().name().equals("CANCELLED"))
                .toList());
        return "workflow/patient";
    }

    @GetMapping("/doctor")
    public String doctorWorkflow(HttpSession session, Model model) {
        User doctor = getUserWithRole(session, Role.DOCTOR);
        if (doctor == null) return "redirect:/login";

        List<CareCaseWorkflow> workflows = careWorkflowService.getDoctorWorkflows(doctor);
        model.addAttribute("doctor", doctor);
        model.addAttribute("workflows", workflows);
        return "workflow/doctor";
    }

    @GetMapping("/case/{appointmentId}")
    public String caseDetail(@PathVariable Long appointmentId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        CareCaseWorkflow workflow = careWorkflowService.getWorkflowForAppointment(
                appointmentId, user.getRole() == Role.PATIENT);
        model.addAttribute("workflow", workflow);
        model.addAttribute("user", user);
        return "workflow/case-detail";
    }

    private User getUserWithRole(HttpSession session, Role role) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null && user.getRole() == role) {
            return user;
        }
        return null;
    }

    private String redirectForRole(Role role) {
        return switch (role) {
            case PATIENT -> "redirect:/workflow/patient";
            case DOCTOR -> "redirect:/workflow/doctor";
            case ADMIN -> "redirect:/admin/dashboard";
            case VENDOR -> "redirect:/vendor/dashboard";
        };
    }
}
