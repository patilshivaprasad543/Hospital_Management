package com.hospital.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

    public String storePatientPhoto(Long patientId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String contentType = file.getContentType() != null ? file.getContentType() : "";
        if (!ALLOWED.contains(contentType)) {
            throw new RuntimeException("Please upload a JPG, PNG, or WEBP photo.");
        }
        if (file.getSize() > 2_000_000) {
            throw new RuntimeException("Photo must be smaller than 2 MB.");
        }
        try {
            Path dir = Path.of("data/uploads/patients").toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String extension = contentType.contains("png") ? ".png" : contentType.contains("webp") ? ".webp" : ".jpg";
            String fileName = patientId + "-" + UUID.randomUUID() + extension;
            Files.copy(file.getInputStream(), dir.resolve(fileName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Could not save profile photo.");
        }
    }

    public String storeDoctorLicense(Long doctorId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > 5_000_000) {
            throw new RuntimeException("License file must be smaller than 5 MB.");
        }
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "license";
        String lower = original.toLowerCase();
        if (!(lower.endsWith(".pdf") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png"))) {
            throw new RuntimeException("Upload a PDF, JPG, or PNG of your medical license.");
        }
        try {
            Path dir = Path.of("data/uploads/doctors").toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String extension = lower.substring(lower.lastIndexOf('.'));
            String fileName = doctorId + "-" + UUID.randomUUID() + extension;
            Files.copy(file.getInputStream(), dir.resolve(fileName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Could not save license document.");
        }
    }
}
