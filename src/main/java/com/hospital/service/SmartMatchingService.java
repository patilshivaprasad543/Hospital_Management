package com.hospital.service;

import com.hospital.model.DoctorProfile;
import com.hospital.model.User;
import com.hospital.repository.DoctorProfileRepository;
import com.hospital.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SmartMatchingService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    public List<Map<String, Object>> findRecommendedDoctors(String symptomCategory) {
        List<User> allDoctors = userRepository.findByRole(com.hospital.model.Role.DOCTOR);
        List<Map<String, Object>> result = new ArrayList<>();

        for (User doc : allDoctors) {
            DoctorProfile profile = doctorProfileRepository.findByUser(doc).orElse(new DoctorProfile(doc));
            String spec = profile.getSpecialization() != null ? profile.getSpecialization().toLowerCase() : "";
            String category = symptomCategory != null ? symptomCategory.toLowerCase() : "";

            int matchScore = 50; // base score

            if (category.contains("skin") && spec.contains("derma")) matchScore += 40;
            else if (category.contains("heart") && spec.contains("cardio")) matchScore += 40;
            else if (category.contains("child") && spec.contains("pedia")) matchScore += 40;
            else if (category.contains("ortho") && spec.contains("ortho")) matchScore += 40;
            else if (category.contains("dental") && spec.contains("dent")) matchScore += 40;
            else if (category.contains("fever") || category.contains("general")) matchScore += 30;

            if (profile.getExperienceYears() != null) {
                matchScore += Math.min(20, profile.getExperienceYears() * 2);
            }

            Map<String, Object> map = new HashMap<>();
            map.put("doctor", doc);
            map.put("profile", profile);
            map.put("score", matchScore);
            result.add(map);
        }

        result.sort((a, b) -> Integer.compare((Integer) b.get("score"), (Integer) a.get("score")));
        return result;
    }
}
