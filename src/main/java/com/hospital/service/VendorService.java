package com.hospital.service;

import com.hospital.model.LabTest;
import com.hospital.model.PharmacyItem;
import com.hospital.model.User;
import com.hospital.repository.LabTestRepository;
import com.hospital.repository.PharmacyItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VendorService {

    @Autowired
    private LabTestRepository labTestRepository;

    @Autowired
    private PharmacyItemRepository pharmacyItemRepository;

    // Lab Tests
    public LabTest saveLabTest(LabTest test) {
        return labTestRepository.save(test);
    }

    public List<LabTest> getLabTestsByVendor(User vendor) {
        return labTestRepository.findByVendor(vendor);
    }

    public List<LabTest> getAllLabTests() {
        return labTestRepository.findAllWithVendor();
    }

    public void deleteLabTest(Long testId) {
        labTestRepository.deleteById(testId);
    }

    // Pharmacy Items
    public PharmacyItem savePharmacyItem(PharmacyItem item) {
        return pharmacyItemRepository.save(item);
    }

    public List<PharmacyItem> getPharmacyItemsByVendor(User vendor) {
        return pharmacyItemRepository.findByVendor(vendor);
    }

    public List<PharmacyItem> getAllPharmacyItems() {
        return pharmacyItemRepository.findAllWithVendor();
    }

    public void deletePharmacyItem(Long itemId) {
        pharmacyItemRepository.deleteById(itemId);
    }

    public PharmacyItem getPharmacyItemForVendor(Long itemId, User vendor) {
        PharmacyItem item = pharmacyItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Medicine not found"));
        if (item.getVendor() == null || vendor == null || !item.getVendor().getId().equals(vendor.getId())) {
            throw new RuntimeException("You are not authorized to update this medicine.");
        }
        return item;
    }
}
