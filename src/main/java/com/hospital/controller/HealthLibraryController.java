package com.hospital.controller;

import com.hospital.model.HealthArticle;
import com.hospital.model.User;
import com.hospital.service.HealthLibraryService;
import com.hospital.service.UserSessionHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/health-library")
public class HealthLibraryController {

    @Autowired
    private HealthLibraryService healthLibraryService;

    @GetMapping({"", "/"})
    public String showLibrary(@RequestParam(value = "query", required = false) String query, HttpSession session, Model model) {
        User loggedInUser = UserSessionHelper.getAnyLoggedInUser(session);
        List<HealthArticle> articles = healthLibraryService.searchArticles(query);

        model.addAttribute("articles", articles);
        model.addAttribute("query", query);
        model.addAttribute("loggedInUser", loggedInUser);
        return "public/health-library";
    }

    @GetMapping("/{id}")
    public String showArticleDetail(@PathVariable("id") Long id, HttpSession session, Model model) {
        User loggedInUser = UserSessionHelper.getAnyLoggedInUser(session);
        HealthArticle article = healthLibraryService.getArticleById(id);
        if (article == null) return "redirect:/health-library";

        model.addAttribute("article", article);
        model.addAttribute("loggedInUser", loggedInUser);
        return "public/article-detail";
    }
}
