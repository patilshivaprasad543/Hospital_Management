package com.hospital.repository;

import com.hospital.model.HealthArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealthArticleRepository extends JpaRepository<HealthArticle, Long> {
    List<HealthArticle> findByCategory(String category);
    List<HealthArticle> findByTitleContainingIgnoreCaseOrSummaryContainingIgnoreCase(String title, String summary);
}
