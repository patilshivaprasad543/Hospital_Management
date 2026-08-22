package com.hospital.controller;

import com.hospital.dto.CareEpisodeDto;
import com.hospital.dto.CarePassportDto;
import com.hospital.dto.QueueStatusDto;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.service.CareEpisodeService;
import com.hospital.service.PdfService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/patient")
public class CareEpisodeController {

    @Autowired
    private CareEpisodeService careEpisodeService;

    @Autowired
    private PdfService pdfService;

    private User getLoggedInPatient(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null && user.getRole() == Role.PATIENT) {
            return user;
        }
        return null;
    }

    @GetMapping("/care-episodes")
    public String listEpisodes(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        List<CareEpisodeDto> episodes = careEpisodeService.getPatientEpisodes(patient);
        model.addAttribute("episodes", episodes);
        model.addAttribute("activeEpisodes", episodes.stream().filter(CareEpisodeDto::isActive).toList());
        return "patient/care-episodes";
    }

    @GetMapping("/care-episodes/{id}")
    public String episodeDetail(@PathVariable Long id, HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        CareEpisodeDto episode = careEpisodeService.getEpisode(id, patient);
        model.addAttribute("episode", episode);
        model.addAttribute("patient", patient);
        return "patient/care-episode-detail";
    }

    @GetMapping("/care-episodes/{id}/queue")
    @ResponseBody
    public QueueStatusDto queueStatus(@PathVariable Long id, HttpSession session) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return new QueueStatusDto();

        CareEpisodeDto episode = careEpisodeService.getEpisode(id, patient);
        return episode.getQueueStatus() != null ? episode.getQueueStatus() : new QueueStatusDto();
    }

    @PostMapping("/care-episodes/{id}/checklist/{key}")
    public String toggleChecklist(@PathVariable Long id,
                                  @PathVariable String key,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        try {
            careEpisodeService.toggleChecklistItem(id, patient, key);
            redirectAttributes.addFlashAttribute("successMessage", "Checklist updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/care-episodes/" + id;
    }

    @GetMapping("/care-passport")
    public String carePassport(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        CarePassportDto passport = careEpisodeService.buildPassport(patient);
        model.addAttribute("passport", passport);
        model.addAttribute("patient", patient);
        return "patient/care-passport";
    }

    @GetMapping("/care-passport/pdf")
    public ResponseEntity<byte[]> downloadCarePassportPdf(HttpSession session) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return ResponseEntity.status(401).build();

        CarePassportDto passport = careEpisodeService.buildPassport(patient);
        byte[] pdf = pdfService.generateCarePassportPdf(passport);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=care-passport-" + passport.getPassportId() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
