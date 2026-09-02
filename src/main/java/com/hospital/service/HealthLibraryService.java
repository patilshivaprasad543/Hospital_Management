package com.hospital.service;

import com.hospital.model.HealthArticle;
import com.hospital.repository.HealthArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HealthLibraryService {

    @Autowired
    private HealthArticleRepository articleRepository;

    @Transactional
    public void seedHealthArticlesIfEmpty() {
        if (articleRepository.count() > 0) return;

        articleRepository.save(new HealthArticle(
                "Cardiovascular Health & Hypertension Guide",
                "Conditions",
                "High blood pressure (hypertension) is a common condition where long-term force against artery walls can lead to heart disease.",
                "Hypertension often develops over years without noticeable symptoms. Regular blood pressure monitoring is essential for early detection.",
                "Headaches, shortness of breath, nosebleeds (in severe cases).",
                "Lifestyle modifications, reduced sodium diet, regular exercise, antihypertensive medications.",
                "Maintain healthy weight, exercise 150 mins weekly, limit alcohol, manage stress.",
                "❤️"
        ));

        articleRepository.save(new HealthArticle(
                "Type 2 Diabetes Management & Prevention",
                "Conditions",
                "Type 2 diabetes affects how your body uses glucose for energy. Proper diet, blood monitoring, and exercise keep blood sugar levels stable.",
                "In type 2 diabetes, the pancreas doesn't produce enough insulin or cells respond poorly to insulin.",
                "Increased thirst, frequent urination, fatigue, blurred vision, slow-healing sores.",
                "Healthy eating, blood sugar monitoring, diabetes medications or insulin therapy.",
                "Eat balanced high-fiber foods, stay active, maintain optimal BMI.",
                "🩸"
        ));

        articleRepository.save(new HealthArticle(
                "Seasonal Influenza & Respiratory Viral Care",
                "Symptoms",
                "Influenza is a viral infection targeting the respiratory system. Early antiviral treatment reduces symptom duration.",
                "Flu spreads via respiratory droplets. Annual flu vaccination is the most effective prevention strategy.",
                "High fever, body aches, dry cough, fatigue, sore throat, nasal congestion.",
                "Rest, hydration, fever reducers, antiviral drugs when prescribed early.",
                "Annual flu vaccine, frequent handwashing, avoiding close contact with sick individuals.",
                "🫁"
        ));

        articleRepository.save(new HealthArticle(
                "Stress Management & Mental Wellness",
                "Prevention",
                "Chronic stress affects both mental and physical health. Mindful techniques and therapy help foster emotional resilience.",
                "Long-term stress elevates cortisol levels, negatively impacting sleep, immune function, and cardiovascular health.",
                "Anxiety, irritability, insomnia, muscle tension, digestive issues.",
                "Cognitive behavioral therapy, mindfulness meditation, exercise, support networks.",
                "Practice daily relaxation, set boundaries, maintain healthy sleep hygiene.",
                "🧠"
        ));
    }

    public List<HealthArticle> getAllArticles() {
        seedHealthArticlesIfEmpty();
        return articleRepository.findAll();
    }

    public HealthArticle getArticleById(Long id) {
        return articleRepository.findById(id).orElse(null);
    }

    public List<HealthArticle> searchArticles(String query) {
        seedHealthArticlesIfEmpty();
        if (query == null || query.isBlank()) {
            return articleRepository.findAll();
        }
        return articleRepository.findByTitleContainingIgnoreCaseOrSummaryContainingIgnoreCase(query.trim(), query.trim());
    }
}
