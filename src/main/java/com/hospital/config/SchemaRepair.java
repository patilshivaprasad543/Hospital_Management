package com.hospital.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class SchemaRepair implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        addColumnIfMissing("appointments", "reminder_sent", "BOOLEAN DEFAULT FALSE NOT NULL");
        addColumnIfMissing("patient_profiles", "photo_file_name", "VARCHAR(255)");
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME)=? AND UPPER(COLUMN_NAME)=?",
                    Integer.class, table.toUpperCase(), column.toUpperCase());
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            }
        } catch (Exception ignored) {
            // Database dialects without INFORMATION_SCHEMA still rely on Hibernate ddl-auto=update.
        }
    }
}
