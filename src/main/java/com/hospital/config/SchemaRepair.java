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
        widenVarchar("pharmacy_orders", "status", 40);
        widenVarchar("pharmacy_orders", "payment_status", 40);
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME)=? AND UPPER(COLUMN_NAME)=?",
                    Integer.class, table.toUpperCase(), column.toUpperCase());
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                System.out.println(">>> SchemaRepair added " + table + "." + column);
            }
        } catch (Exception ignored) {
            // Database dialects without INFORMATION_SCHEMA still rely on Hibernate ddl-auto=update.
        }
    }

    private void widenVarchar(String table, String column, int length) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN " + column
                    + " SET DATA TYPE VARCHAR(" + length + ")");
            System.out.println(">>> SchemaRepair widened " + table + "." + column + " to VARCHAR(" + length + ")");
        } catch (Exception e) {
            try {
                jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN " + column
                        + " VARCHAR(" + length + ")");
                System.out.println(">>> SchemaRepair altered " + table + "." + column + " to VARCHAR(" + length + ")");
            } catch (Exception ignored2) {
                System.out.println(">>> SchemaRepair could not widen " + table + "." + column + ": " + e.getMessage());
            }
        }
    }
}
