package com.hospital.controller;

import com.hospital.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        model.addAttribute("activePage", "home");
        addPublicModel(session, model);
        return "public/home";
    }

    @GetMapping("/about")
    public String about(HttpSession session, Model model) {
        model.addAttribute("activePage", "about");
        addPublicModel(session, model);
        return "public/about";
    }

    @GetMapping("/contact")
    public String contact(HttpSession session, Model model) {
        model.addAttribute("activePage", "contact");
        addPublicModel(session, model);
        return "public/contact";
    }

    @GetMapping("/download-app")
    public String downloadApp(jakarta.servlet.http.HttpServletRequest request, HttpSession session, Model model) {
        model.addAttribute("activePage", "download-app");
        addPublicModel(session, model);

        String currentFullUrl = request.getRequestURL().toString();
        model.addAttribute("currentFullUrl", currentFullUrl);

        // Serves direct local APK or redirects to latest GitHub release APK
        model.addAttribute("apkDownloadUrl", "/download/apk");

        return "public/download-app";
    }

    @GetMapping("/download/apk")
    public org.springframework.http.ResponseEntity<?> downloadApk() {
        java.io.File localApk = new java.io.File("data/downloads/SmartCare360.apk");
        if (localApk.exists()) {
            org.springframework.core.io.FileSystemResource resource = new org.springframework.core.io.FileSystemResource(localApk);
            return org.springframework.http.ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"SmartCare360.apk\"")
                    .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.android.package-archive"))
                    .body(resource);
        }

        return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                .location(java.net.URI.create("https://github.com/patilshivaprasad543/Hospital_Management/releases/latest"))
                .build();
    }

    @PostMapping("/contact")
    public String submitContact(@RequestParam("name") String name,
                                @RequestParam("email") String email,
                                @RequestParam("subject") String subject,
                                @RequestParam("message") String message,
                                RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("successMessage",
                "Thank you, " + name + "! Your message has been received. Our team will respond to " + email + " shortly.");
        return "redirect:/contact";
    }

    private void addPublicModel(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        model.addAttribute("loggedInUser", user);
        model.addAttribute("dashboardUrl", dashboardUrlForUser(user));
    }

    public static String dashboardUrlForUser(User user) {
        if (user == null) {
            return "/login";
        }
        String redirect = AuthController.getRedirectUrlForRole(user.getRole());
        return redirect.startsWith("redirect:") ? redirect.substring("redirect:".length()) : redirect;
    }
}
