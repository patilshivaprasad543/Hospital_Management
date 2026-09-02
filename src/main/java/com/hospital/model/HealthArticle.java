package com.hospital.model;

import jakarta.persistence.*;

@Entity
@Table(name = "health_articles")
public class HealthArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String category; // Symptoms, Conditions, Prevention, Procedures, Nutrition

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String symptomsInfo;

    @Column(columnDefinition = "TEXT")
    private String treatmentInfo;

    @Column(columnDefinition = "TEXT")
    private String preventionTips;

    private String iconEmoji;

    public HealthArticle() {}

    public HealthArticle(String title, String category, String summary, String content, String symptomsInfo, String treatmentInfo, String preventionTips, String iconEmoji) {
        this.title = title;
        this.category = category;
        this.summary = summary;
        this.content = content;
        this.symptomsInfo = symptomsInfo;
        this.treatmentInfo = treatmentInfo;
        this.preventionTips = preventionTips;
        this.iconEmoji = iconEmoji;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSymptomsInfo() { return symptomsInfo; }
    public void setSymptomsInfo(String symptomsInfo) { this.symptomsInfo = symptomsInfo; }

    public String getTreatmentInfo() { return treatmentInfo; }
    public void setTreatmentInfo(String treatmentInfo) { this.treatmentInfo = treatmentInfo; }

    public String getPreventionTips() { return preventionTips; }
    public void setPreventionTips(String preventionTips) { this.preventionTips = preventionTips; }

    public String getIconEmoji() { return iconEmoji; }
    public void setIconEmoji(String iconEmoji) { this.iconEmoji = iconEmoji; }
}
