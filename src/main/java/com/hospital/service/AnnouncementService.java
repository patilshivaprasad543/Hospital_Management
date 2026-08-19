package com.hospital.service;

import com.hospital.model.Announcement;
import com.hospital.model.User;
import com.hospital.repository.AnnouncementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class AnnouncementService {

    @Autowired
    private AnnouncementRepository announcementRepository;

    public List<Announcement> getActiveForRole(String role) {
        if (role == null) {
            return announcementRepository.findByActiveTrueOrderByCreatedAtDesc();
        }
        return announcementRepository.findByActiveTrueAndAudienceInOrderByCreatedAtDesc(
                Arrays.asList("ALL", role.toUpperCase()));
    }

    public List<Announcement> getAll() {
        return announcementRepository.findAll();
    }

    public Announcement create(String title, String message, String audience, User admin) {
        Announcement a = new Announcement();
        a.setTitle(title);
        a.setMessage(message);
        a.setAudience(audience != null ? audience : "ALL");
        a.setCreatedBy(admin);
        return announcementRepository.save(a);
    }

    public void toggleActive(Long id) {
        announcementRepository.findById(id).ifPresent(a -> {
            a.setActive(!a.isActive());
            announcementRepository.save(a);
        });
    }

    public void delete(Long id) {
        announcementRepository.deleteById(id);
    }
}
