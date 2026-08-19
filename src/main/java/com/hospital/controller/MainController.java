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
