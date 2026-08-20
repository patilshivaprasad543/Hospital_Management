package com.hospital.service;

import com.hospital.model.HospitalSetting;
import com.hospital.repository.HospitalSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class HospitalSettingService {

    public static final String HOSPITAL_NAME = "hospital.name";
    public static final String HOSPITAL_ADDRESS = "hospital.address";
    public static final String HOSPITAL_PHONE = "hospital.phone";
    public static final String HOSPITAL_EMAIL = "hospital.email";
    public static final String HOSPITAL_HOURS = "hospital.workingHours";
    public static final String EMAIL_NOTIFICATIONS = "notifications.emailEnabled";

    @Autowired
    private HospitalSettingRepository hospitalSettingRepository;

    public void ensureDefaults() {
        putIfAbsent(HOSPITAL_NAME, "SmartCare 360 Hospital");
        putIfAbsent(HOSPITAL_ADDRESS, "Main Hospital Block, Metro City");
        putIfAbsent(HOSPITAL_PHONE, "1800-360-CARE");
        putIfAbsent(HOSPITAL_EMAIL, "admin@smartcare360.com");
        putIfAbsent(HOSPITAL_HOURS, "24x7 Emergency · OPD 08:00 AM – 08:00 PM");
        putIfAbsent(EMAIL_NOTIFICATIONS, "true");
    }

    public Map<String, String> getAllSettings() {
        ensureDefaults();
        Map<String, String> values = new LinkedHashMap<>();
        values.put(HOSPITAL_NAME, get(HOSPITAL_NAME));
        values.put(HOSPITAL_ADDRESS, get(HOSPITAL_ADDRESS));
        values.put(HOSPITAL_PHONE, get(HOSPITAL_PHONE));
        values.put(HOSPITAL_EMAIL, get(HOSPITAL_EMAIL));
        values.put(HOSPITAL_HOURS, get(HOSPITAL_HOURS));
        values.put(EMAIL_NOTIFICATIONS, get(EMAIL_NOTIFICATIONS));
        return values;
    }

    public String get(String key) {
        return hospitalSettingRepository.findBySettingKey(key)
                .map(HospitalSetting::getSettingValue)
                .orElse("");
    }

    public void save(String key, String value) {
        HospitalSetting setting = hospitalSettingRepository.findBySettingKey(key)
                .orElseGet(() -> new HospitalSetting(key, value));
        setting.setSettingValue(value != null ? value.trim() : "");
        hospitalSettingRepository.save(setting);
    }

    public void saveAll(String name, String address, String phone, String email, String hours, boolean emailEnabled) {
        save(HOSPITAL_NAME, name);
        save(HOSPITAL_ADDRESS, address);
        save(HOSPITAL_PHONE, phone);
        save(HOSPITAL_EMAIL, email);
        save(HOSPITAL_HOURS, hours);
        save(EMAIL_NOTIFICATIONS, Boolean.toString(emailEnabled));
    }

    private void putIfAbsent(String key, String value) {
        if (hospitalSettingRepository.findBySettingKey(key).isEmpty()) {
            hospitalSettingRepository.save(new HospitalSetting(key, value));
        }
    }
}
