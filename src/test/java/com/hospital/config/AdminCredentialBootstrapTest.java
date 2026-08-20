package com.hospital.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminCredentialBootstrapTest {

    @TempDir
    Path tempDir;

    @Test
    void prodWithoutSecretsGeneratesAndReusesPassword() throws Exception {
        MockEnvironment first = new MockEnvironment();
        first.setActiveProfiles("prod");
        first.setProperty("SMARTCARE_DATA_DIR", tempDir.toString());

        new AdminCredentialBootstrap().postProcessEnvironment(first, new SpringApplication());

        String email = first.getProperty("smartcare.admin.email");
        String password = first.getProperty("smartcare.admin.password");
        assertEquals("admin@smartcare360.local", email);
        assertFalse(password.isBlank());
        assertNotEquals(AdminCredentialBootstrap.WEAK_DEFAULT_PASSWORD, password);
        assertTrue(Files.isRegularFile(tempDir.resolve("admin-bootstrap.properties")));

        MockEnvironment second = new MockEnvironment();
        second.setActiveProfiles("prod");
        second.setProperty("SMARTCARE_DATA_DIR", tempDir.toString());
        new AdminCredentialBootstrap().postProcessEnvironment(second, new SpringApplication());
        assertEquals(password, second.getProperty("smartcare.admin.password"));
    }
}
