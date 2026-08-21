package com.hospital.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalEnvLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsDotEnvIntoSystemProperties() throws Exception {
        Path env = tempDir.resolve("local.env");
        Files.writeString(env, "SMARTCARE_TEST_ECLIPSE_KEY=eclipse-sql\n");
        System.clearProperty("SMARTCARE_TEST_ECLIPSE_KEY");
        LocalEnvLoader.load(env);
        assertEquals("eclipse-sql", System.getProperty("SMARTCARE_TEST_ECLIPSE_KEY"));
        System.clearProperty("SMARTCARE_TEST_ECLIPSE_KEY");
    }
}
