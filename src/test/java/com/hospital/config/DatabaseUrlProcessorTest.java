package com.hospital.config;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseUrlProcessorTest {

    @Test
    void convertsRenderPostgresUrl() {
        Map<String, Object> props = new LinkedHashMap<>();
        DatabaseUrlProcessor.applyExternalUrl(
                "postgres://smartcare:s3cret@dpg-host:5432/smartcare360", props);
        assertEquals("org.postgresql.Driver", props.get("spring.datasource.driver-class-name"));
        assertEquals("smartcare", props.get("spring.datasource.username"));
        assertEquals("s3cret", props.get("spring.datasource.password"));
        assertTrue(String.valueOf(props.get("spring.datasource.url")).startsWith("jdbc:postgresql://dpg-host:5432/smartcare360"));
        assertTrue(String.valueOf(props.get("spring.datasource.url")).contains("sslmode=require"));
    }

    @Test
    void prodWithoutExternalUrlUsesFileDatabase() {
        Map<String, Object> props = DatabaseUrlProcessor.configure(null, true);
        assertTrue(String.valueOf(props.get("spring.datasource.url")).contains("jdbc:h2:file:"));
        assertEquals("org.h2.Driver", props.get("spring.datasource.driver-class-name"));
    }
}
