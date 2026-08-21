package com.hospital.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProjectDownloadController {

    private static final String ZIP_PATH = "static/downloads/Hospital_Management_Eclipse.zip";
    private static final String FILE_NAME = "Hospital_Management_Eclipse.zip";

    @GetMapping("/download/eclipse")
    public ResponseEntity<Resource> downloadEclipseZip() {
        Resource zip = new ClassPathResource(ZIP_PATH);
        if (!zip.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + FILE_NAME + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }
}
