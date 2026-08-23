package com.hospital.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class LocaleConfigTest {

    @Autowired
    private LocaleResolver localeResolver;

    @Autowired
    private LocaleChangeInterceptor localeChangeInterceptor;

    @Autowired
    private MessageSource messageSource;

    @Test
    void localeBeansAreRegistered() {
        assertNotNull(localeResolver);
        assertNotNull(localeChangeInterceptor);
        assertEquals(LocaleConfig.LOCALE_PARAM, localeChangeInterceptor.getParamName());
    }

    @Test
    void hindiMessagesResolve() {
        String hindi = messageSource.getMessage("nav.dashboard", null, Locale.forLanguageTag("hi"));
        assertEquals("डैशबोर्ड", hindi);
    }

    @Test
    void teluguMessagesResolve() {
        String telugu = messageSource.getMessage("nav.dashboard", null, Locale.forLanguageTag("te"));
        assertEquals("డాష్‌బోర్డ్", telugu);
    }

    @Test
    void kannadaMessagesResolve() {
        String kannada = messageSource.getMessage("nav.dashboard", null, Locale.forLanguageTag("kn"));
        assertEquals("ಡ್ಯಾಶ್‌ಬೋರ್ಡ್", kannada);
    }

    @Test
    void englishMessagesResolve() {
        String english = messageSource.getMessage("language.preferences", null, Locale.ENGLISH);
        assertEquals("Language Preferences", english);
    }
}
