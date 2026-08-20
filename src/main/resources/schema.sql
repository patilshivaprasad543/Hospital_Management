# ============================================================
# SmartCare 360 — Database Schema (schema.sql)
# Compatible with H2 / MySQL / MariaDB
# OTP codes are NOT stored — handled in-memory by OtpService
# Admin user is NOT seeded here — created from env via DataInitializer
# ============================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    mobile_number VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    vendor_type VARCHAR(50) DEFAULT 'NONE',
    verified BOOLEAN DEFAULT FALSE,
    account_status VARCHAR(50) DEFAULT 'ACTIVE',
    approval_status VARCHAR(50) DEFAULT 'PENDING_OTP',
    admin_approved BOOLEAN DEFAULT FALSE,
    document_info VARCHAR(2000),
    last_login_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500),
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS patient_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    blood_group VARCHAR(10),
    age INT,
    gender VARCHAR(20),
    address VARCHAR(500),
    date_of_birth DATE,
    emergency_contact_name VARCHAR(255),
    emergency_contact_phone VARCHAR(50),
    allergies VARCHAR(500),
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
    slot_duration_minutes INT DEFAULT 30,
    working_days VARCHAR(100) DEFAULT 'MON,TUE,WED,THU,FRI,SAT',
    work_start_time VARCHAR(10) DEFAULT '09:00',
    work_end_time VARCHAR(10) DEFAULT '17:00',
    max_appointments_per_day INT DEFAULT 16,
    department_id BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
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

CREATE TABLE IF NOT EXISTS doctor_leaves (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    leave_date DATE NOT NULL,
    reason VARCHAR(500),
    FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    audience VARCHAR(50) NOT NULL DEFAULT 'ALL',
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by_id BIGINT,
    FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE SET NULL
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
    queue_ticket VARCHAR(50),
    department_category VARCHAR(100),
    state VARCHAR(50) DEFAULT 'PENDING_DOCTOR_APPROVAL',
    checked_in_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS consultations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE,
    doctor_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    symptoms VARCHAR(2000),
    diagnosis VARCHAR(2000),
    treatment VARCHAR(2000),
    notes VARCHAR(2000),
    follow_up_date DATE,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS consultation_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    rating INT,
    comment VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS prescriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT,
    doctor_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    diagnosis VARCHAR(500),
    instructions VARCHAR(1000),
    follow_up_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,
    FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS prescription_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    medicine_name VARCHAR(255) NOT NULL,
    dosage VARCHAR(100),
    frequency VARCHAR(100),
    duration VARCHAR(100),
    instructions VARCHAR(500),
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE
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

CREATE TABLE IF NOT EXISTS lab_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    lab_test_id BIGINT,
    lab_vendor_id BIGINT,
    test_name VARCHAR(255),
    status VARCHAR(50) DEFAULT 'REQUESTED',
    report_result VARCHAR(2000),
    notes VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (lab_test_id) REFERENCES lab_tests(id) ON DELETE SET NULL,
    FOREIGN KEY (lab_vendor_id) REFERENCES users(id) ON DELETE SET NULL
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

CREATE TABLE IF NOT EXISTS pharmacy_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    prescription_id BIGINT,
    pharmacy_vendor_id BIGINT,
    total_price DOUBLE,
    status VARCHAR(50) DEFAULT 'PLACED',
    order_summary VARCHAR(1000),
    delivery_address VARCHAR(500),
    tracking_notes VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE SET NULL,
    FOREIGN KEY (pharmacy_vendor_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    invoice_number VARCHAR(100),
    charge_type VARCHAR(50),
    description VARCHAR(1000),
    amount DOUBLE,
    payment_status VARCHAR(50) DEFAULT 'PENDING',
    reference_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    amount DOUBLE,
    status VARCHAR(50) DEFAULT 'PAID',
    transaction_ref VARCHAR(100),
    paid_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    title VARCHAR(255),
    message VARCHAR(1000),
    category VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    link_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    module VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    details VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS notification_dispatch_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(500),
    body VARCHAR(4000),
    delivered BOOLEAN NOT NULL,
    note VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_dispatch_log_created ON notification_dispatch_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_dispatch_log_recipient ON notification_dispatch_logs(recipient);
