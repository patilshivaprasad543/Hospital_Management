package com.hospital.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Path.of("data/uploads").toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDir.toUri().toString());

        // Cache static CSS, JS, and image assets for 7 days in the browser for instant repeat loads
        registry.addResourceHandler("/css/**", "/js/**", "/images/**")
                .addResourceLocations("classpath:/static/css/", "classpath:/static/js/", "classpath:/static/images/")
                .setCacheControl(org.springframework.http.CacheControl.maxAge(7, java.util.concurrent.TimeUnit.DAYS).cachePublic());

        // PWA Manifest and Service Worker
        registry.addResourceHandler("/manifest.webmanifest", "/manifest.json", "/service-worker.js")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(org.springframework.http.CacheControl.noCache());
    }
}
