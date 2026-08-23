package com.hospital.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Locale;
import java.util.Set;

@Controller
@RequestMapping("/patient")
public class LanguageController {

    private static final Set<String> SUPPORTED = Set.of("en", "hi", "te", "kn");

    @PostMapping("/language")
    public String changeLanguage(@RequestParam("lang") String lang,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        String normalized = lang != null ? lang.toLowerCase(Locale.ROOT) : "en";
        if (!SUPPORTED.contains(normalized)) {
            normalized = "en";
        }

        LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
        if (localeResolver != null) {
            localeResolver.setLocale(request, response, Locale.forLanguageTag(normalized));
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }
        return "redirect:/patient/dashboard";
    }
}
