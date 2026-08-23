package com.hospital.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class LocaleModelAdvice {

    public record LocaleOption(String code, String labelKey) {}

    @ModelAttribute("localeOptions")
    public List<LocaleOption> localeOptions() {
        return List.of(
                new LocaleOption("en", "language.english"),
                new LocaleOption("hi", "language.hindi")
        );
    }
}
