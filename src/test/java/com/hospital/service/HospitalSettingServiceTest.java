package com.hospital.service;

import com.hospital.model.HospitalSetting;
import com.hospital.repository.HospitalSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HospitalSettingServiceTest {

    @Mock
    private HospitalSettingRepository hospitalSettingRepository;

    @InjectMocks
    private HospitalSettingService hospitalSettingService;

    private final Map<String, HospitalSetting> store = new HashMap<>();

    @BeforeEach
    void stubRepository() {
        store.clear();
        when(hospitalSettingRepository.findBySettingKey(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(store.get(invocation.getArgument(0))));
        when(hospitalSettingRepository.save(any(HospitalSetting.class))).thenAnswer(invocation -> {
            HospitalSetting setting = invocation.getArgument(0);
            store.put(setting.getSettingKey(), setting);
            return setting;
        });
    }

    @Test
    void saveAllPersistsHospitalConfiguration() {
        hospitalSettingService.saveAll("City Hospital", "Main Road", "1800", "ops@hospital.com", "9-5", true);
        assertEquals("City Hospital", hospitalSettingService.get(HospitalSettingService.HOSPITAL_NAME));
        assertEquals("Main Road", hospitalSettingService.get(HospitalSettingService.HOSPITAL_ADDRESS));
        assertEquals("true", hospitalSettingService.get(HospitalSettingService.EMAIL_NOTIFICATIONS));
    }
}
