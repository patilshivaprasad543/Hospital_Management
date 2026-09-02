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
        addColumnIfMissing("appointments", "consultation_type", "VARCHAR(50) DEFAULT 'IN_PERSON' NOT NULL");
        addColumnIfMissing("appointments", "video_room_id", "VARCHAR(255)");
        addColumnIfMissing("appointments", "video_status", "VARCHAR(50)");
        addColumnIfMissing("appointments", "video_join_available_from", "DATETIME");
        addColumnIfMissing("appointments", "video_join_expires_at", "DATETIME");

        addColumnIfMissing("patient_profiles", "photo_file_name", "VARCHAR(255)");
        addColumnIfMissing("users", "rejection_reason", "VARCHAR(1000)");
        addColumnIfMissing("doctor_profiles", "hospital_name", "VARCHAR(255)");
        addColumnIfMissing("doctor_profiles", "clinic_address", "VARCHAR(500)");
        addColumnIfMissing("doctor_profiles", "license_number", "VARCHAR(255)");
        addColumnIfMissing("doctor_profiles", "license_file_name", "VARCHAR(255)");
        addColumnIfMissing("consultations", "observations", "VARCHAR(2000)");
        addColumnIfMissing("vendor_profiles", "owner_name", "VARCHAR(255)");
        addColumnIfMissing("vendor_profiles", "license_number", "VARCHAR(255)");
        addColumnIfMissing("vendor_profiles", "license_file_name", "VARCHAR(255)");
        addColumnIfMissing("vendor_profiles", "working_hours", "VARCHAR(255)");
        addColumnIfMissing("vendor_profiles", "delivery_area", "VARCHAR(500)");
        addColumnIfMissing("pharmacy_items", "manufacturer", "VARCHAR(255)");
        addColumnIfMissing("pharmacy_items", "batch_number", "VARCHAR(255)");
        addColumnIfMissing("pharmacy_items", "expiry_date", "DATE");
        addColumnIfMissing("pharmacy_items", "low_stock_threshold", "INT DEFAULT 10");
        addColumnIfMissing("pharmacy_orders", "prescription_verified", "BOOLEAN DEFAULT FALSE NOT NULL");
        addColumnIfMissing("pharmacy_orders", "stock_checked", "BOOLEAN DEFAULT FALSE NOT NULL");
        addColumnIfMissing("pharmacy_orders", "stock_deducted", "BOOLEAN DEFAULT FALSE NOT NULL");
        addColumnIfMissing("pharmacy_orders", "invoice_id", "BIGINT");
        addColumnIfMissing("pharmacy_orders", "payment_status", "VARCHAR(50)");
        addColumnIfMissing("pharmacy_orders", "pharmacy_notes", "VARCHAR(500)");

        widenVarchar("appointments", "status", 50);
        widenVarchar("appointments", "video_status", 50);
        widenVarchar("appointments", "consultation_type", 50);
        widenVarchar("users", "approval_status", 50);
        widenVarchar("users", "account_status", 50);
        widenVarchar("pharmacy_orders", "status", 50);
        widenVarchar("pharmacy_orders", "payment_status", 50);
        widenVarchar("ambulances", "status", 50);
        widenVarchar("ambulances", "type", 50);
        widenVarchar("ambulance_trips", "status", 50);
        widenVarchar("ambulance_trips", "priority", 50);
        widenVarchar("ambulance_trips", "requested_type", 50);
        widenVarchar("notifications", "category", 50);

        try {
            jdbcTemplate.execute("DELETE FROM doctor_profiles WHERE user_id IN (SELECT id FROM users WHERE LOWER(email) = 'viratshiva187@gmail.com')");
            jdbcTemplate.execute("DELETE FROM users WHERE LOWER(email) = 'viratshiva187@gmail.com'");
        } catch (Exception ignored) {}
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        try {
            Integer count = columnCount(table, column);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                System.out.println(">>> SchemaRepair added " + table + "." + column);
            }
        } catch (Exception ignored) {
            // Database dialects without INFORMATION_SCHEMA still rely on Hibernate ddl-auto=update.
        }
    }

    private Integer columnCount(String table, String column) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME)=? AND UPPER(COLUMN_NAME)=?",
                    Integer.class, table.toUpperCase(), column.toUpperCase());
        } catch (Exception mysqlOrMissing) {
            try {
                return jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME)=? AND UPPER(COLUMN_NAME)=?",
                        Integer.class, table.toUpperCase(), column.toUpperCase());
            } catch (Exception e) {
                return 0;
            }
        }
    }

    private void widenVarchar(String table, String column, int length) {
        try {
            Integer charLen = jdbcTemplate.queryForObject(
                    "SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME)=? AND UPPER(COLUMN_NAME)=?",
                    Integer.class, table.toUpperCase(), column.toUpperCase());
            if (charLen != null && charLen >= length) {
                return; // Already widened, avoid slow ALTER TABLE lock
            }
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " MODIFY COLUMN " + column + " VARCHAR(" + length + ")");
            System.out.println(">>> SchemaRepair modified " + table + "." + column + " to VARCHAR(" + length + ")");
        } catch (Exception e) {
            try {
                jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN " + column
                        + " VARCHAR(" + length + ")");
                System.out.println(">>> SchemaRepair altered " + table + "." + column + " to VARCHAR(" + length + ")");
            } catch (Exception ignored2) {
                // Ignore if column doesn't exist yet
            }
        }
    }
}
