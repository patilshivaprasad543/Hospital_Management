-- Local SQL database for SmartCare 360 (MySQL 8 / MariaDB / XAMPP / WAMP)
-- Run this once in MySQL Workbench, phpMyAdmin, or: mysql -u root -p < sql/smartcare360-mysql.sql
-- Hibernate then creates/updates tables (spring.jpa.hibernate.ddl-auto=update).

CREATE DATABASE IF NOT EXISTS smartcare360
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Optional dedicated user (skip if you use root):
-- CREATE USER IF NOT EXISTS 'smartcare'@'localhost' IDENTIFIED BY 'smartcare';
-- GRANT ALL PRIVILEGES ON smartcare360.* TO 'smartcare'@'localhost';
-- FLUSH PRIVILEGES;

USE smartcare360;
