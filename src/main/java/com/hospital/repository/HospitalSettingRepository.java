package com.hospital.repository;

import com.hospital.model.HospitalSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HospitalSettingRepository extends JpaRepository<HospitalSetting, Long> {
    Optional<HospitalSetting> findBySettingKey(String settingKey);
}
