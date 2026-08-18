-- ============================================================
-- Hospital Management System - Initial Data Script (data.sql)
-- Seeds Admin, Doctors, Patients, Vendors, Lab Tests & Medicines
-- ============================================================

-- 0. Cleanup specific user email if present
DELETE FROM users WHERE email = 'viratshiva187@gmail.com';

-- 1. Insert Users

INSERT INTO users (id, full_name, email, mobile_number, password, role, vendor_type, verified) VALUES
(1, 'System Admin', 'admin@hospital.com', '9876543210', 'admin123', 'ADMIN', 'NONE', TRUE),
(2, 'Dr. Sarah Jenkins', 'sarah.jenkins@hospital.com', '9876543211', 'doc123', 'DOCTOR', 'NONE', TRUE),
(3, 'Dr. Robert Chen', 'robert.chen@hospital.com', '9876543212', 'doc123', 'DOCTOR', 'NONE', TRUE),
(4, 'Dr. Emily Watson', 'emily.watson@hospital.com', '9876543213', 'doc123', 'DOCTOR', 'NONE', TRUE),
(5, 'John Doe', 'patient@hospital.com', '9876543214', 'patient123', 'PATIENT', 'NONE', TRUE),
(6, 'Apex Diagnostics Lab', 'lab@hospital.com', '9876543215', 'vendor123', 'VENDOR', 'LABORATORY', TRUE),
(7, 'MediPlus Pharmacy', 'pharmacy@hospital.com', '9876543216', 'vendor123', 'VENDOR', 'PHARMACY', TRUE);

-- 2. Insert Doctor Profiles
INSERT INTO doctor_profiles (user_id, qualification, specialization, experience_years, availability_schedule, consultation_fee) VALUES
(2, 'MD, DM (Cardiology)', 'Cardiology', 12, 'Mon - Sat (09:00 AM - 02:00 PM)', 800.00),
(3, 'MBBS, M.Ch (Neurology)', 'Neurology', 9, 'Mon - Fri (10:00 AM - 04:00 PM)', 1000.00),
(4, 'MBBS, DCH, MD (Pediatrics)', 'Pediatrics', 7, 'Mon - Sat (11:00 AM - 05:00 PM)', 600.00);

-- 3. Insert Patient Profile
INSERT INTO patient_profiles (user_id, blood_group, age, gender, address, medical_history) VALUES
(5, 'O+', 34, 'Male', '123 Health Ave, Metro City', 'No major chronic conditions.');

-- 4. Insert Vendor Profiles
INSERT INTO vendor_profiles (user_id, business_name, vendor_type, address, contact_phone, description) VALUES
(6, 'Apex Pathology & Imaging Lab', 'LABORATORY', 'Block C, Medical Hub', '1800-LAB-TEST', 'NABL Accredited Diagnostic Laboratory offering complete blood tests & body scans.'),
(7, 'MediPlus Central Pharmacy', 'PHARMACY', 'Ground Floor, Main Hospital Block', '1800-MED-PLUS', '24x7 Authorized Hospital Pharmacy providing essential prescription medicines.');

-- 5. Insert Sample Appointments
INSERT INTO appointments (patient_id, doctor_id, appointment_date, appointment_time, status, reason, notes) VALUES
(5, 2, '2026-08-20', '10:00:00', 'CONFIRMED', 'Routine heart checkup', 'Patient advised to bring previous ECG report.'),
(5, 3, '2026-08-25', '11:30:00', 'PENDING', 'Migraine and headache consultation', NULL);

-- 6. Insert Lab Tests Catalog
INSERT INTO lab_tests (test_name, category, price, description, vendor_id) VALUES
('Complete Blood Count (CBC)', 'Blood Test', 450.00, 'Includes RBC, WBC, Hemoglobin, Platelet count.', 6),
('Lipid Profile Test', 'Cardiology', 900.00, 'Measures Total Cholesterol, HDL, LDL, Triglycerides.', 6),
('Thyroid Profile (T3, T4, TSH)', 'Endocrinology', 650.00, 'Comprehensive evaluation of thyroid hormones.', 6);

-- 7. Insert Pharmacy Catalog
INSERT INTO pharmacy_items (item_name, category, price, stock_quantity, description, vendor_id) VALUES
('Paracetamol 650mg', 'Analgesic', 35.00, 500, 'Pain relief & antipyretic strip of 15 tablets.', 7),
('Amoxicillin 500mg', 'Antibiotics', 120.00, 200, 'Broad-spectrum antibiotic strip of 10 capsules.', 7),
('Multivitamin & Mineral', 'Supplements', 210.00, 150, 'Daily health supplement bottle of 30 tablets.', 7);
