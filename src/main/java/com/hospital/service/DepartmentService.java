package com.hospital.service;

import com.hospital.model.Department;
import com.hospital.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    public List<Department> getActiveDepartments() {
        return departmentRepository.findByActiveTrueOrderByNameAsc();
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Department save(Department department) {
        return departmentRepository.save(department);
    }

    public Department findById(Long id) {
        return departmentRepository.findById(id).orElse(null);
    }

    public void seedDepartmentsIfEmpty() {
        if (departmentRepository.count() > 0) {
            return;
        }
        String[][] data = {
                {"General Medicine", "Primary care and general health"},
                {"Cardiology", "Heart and cardiovascular system"},
                {"Neurology", "Brain and nervous system"},
                {"Orthopedics", "Bones, joints and muscles"},
                {"Pediatrics", "Child healthcare"},
                {"Dermatology", "Skin conditions"},
                {"ENT", "Ear, nose and throat"},
                {"Gynecology", "Women's health"},
                {"Radiology", "Imaging and scans"},
                {"Emergency", "Emergency and trauma care"}
        };
        for (String[] row : data) {
            departmentRepository.save(new Department(row[0], row[1]));
        }
    }
}
