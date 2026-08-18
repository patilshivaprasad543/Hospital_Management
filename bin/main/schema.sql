-- ============================================================
-- Hospital Management System - Database Schema (schema.sql)
-- Compatible with MySQL / MariaDB / H2 Database
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    mobile_number VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    vendor_type VARCHAR(50) DEFAULT 'NONE',
    verified BOOLEAN DEFAULT FALSE,
    otp_code VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS patient_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    blood_group VARCHAR(10),
    age INT,
    gender VARCHAR(20),
    address VARCHAR(500),
    medical_history VARCHAR(1000),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS doctor_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    qualification VARCHAR(255),
    specialization VARCHAR(255),
    experience_years INT,
    availability_schedule VARCHAR(255),
    consultation_fee DOUBLE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS vendor_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    business_name VARCHAR(255),
    vendor_type VARCHAR(50),
    address VARCHAR(500),
    contact_phone VARCHAR(50),
    description VARCHAR(1000),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(500),
    notes VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS lab_tests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_name VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    price DOUBLE,
    description VARCHAR(1000),
    vendor_id BIGINT,
    FOREIGN KEY (vendor_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS pharmacy_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_name VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    price DOUBLE,
    stock_quantity INT,
    description VARCHAR(1000),
    vendor_id BIGINT,
    FOREIGN KEY (vendor_id) REFERENCES users(id) ON DELETE SET NULL
);
