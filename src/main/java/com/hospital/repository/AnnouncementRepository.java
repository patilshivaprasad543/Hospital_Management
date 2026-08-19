package com.hospital.repository;

import com.hospital.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByActiveTrueOrderByCreatedAtDesc();
    List<Announcement> findByActiveTrueAndAudienceInOrderByCreatedAtDesc(List<String> audiences);
}
