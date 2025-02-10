package com.rayvision.inventory_management.service.impl;


import com.rayvision.inventory_management.model.Supplier;
import com.rayvision.inventory_management.model.SupplierPhone;
import com.rayvision.inventory_management.repository.SupplierPhoneRepository;
import com.rayvision.inventory_management.repository.SupplierRepository;
import com.rayvision.inventory_management.service.SupplierPhoneService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SupplierPhoneServiceImpl implements SupplierPhoneService {
    private final SupplierPhoneRepository supplierPhoneRepository;
    private final SupplierRepository supplierRepository;

    public SupplierPhoneServiceImpl(SupplierPhoneRepository supplierPhoneRepository, SupplierRepository supplierRepository) {
        this.supplierPhoneRepository = supplierPhoneRepository;
        this.supplierRepository = supplierRepository;
    }

    @Override
    public List<SupplierPhone> getPhonesBySupplier(Long companyId, Long supplierId) {
        // Optionally: Validate that the supplier belongs to the company.
        return supplierPhoneRepository.findBySupplierId(supplierId);
    }

    @Override
    public Optional<SupplierPhone> getPhoneById(Long companyId, Long supplierId, Long phoneId) {
        return supplierPhoneRepository.findBySupplierIdAndId(supplierId, phoneId);
    }

    @Override
    public SupplierPhone savePhone(Long companyId, Long supplierId, SupplierPhone phone) {
        // Ensure the supplier exists and belongs to the company:
        Supplier supplier = supplierRepository.findByCompanyIdAndId(companyId, supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found for company: " + companyId));
        phone.setSupplier(supplier);
        return supplierPhoneRepository.save(phone);
    }

    @Override
    public SupplierPhone updatePhone(Long companyId, Long supplierId, SupplierPhone phone) {
        return supplierPhoneRepository.findBySupplierIdAndId(supplierId, phone.getId())
                .map(existingPhone -> {
                    if (phone.getPhoneNumber() != null) {
                        existingPhone.setPhoneNumber(phone.getPhoneNumber());
                    }
                    // Always update the default flag, even if false.
                    existingPhone.setDefault(phone.isDefault());
                    return supplierPhoneRepository.save(existingPhone);
                }).orElseThrow(() -> new RuntimeException("Supplier phone not found for supplier: " + supplierId));
    }

    @Override
    public void deletePhone(Long companyId, Long supplierId, Long phoneId) {
        SupplierPhone phone = supplierPhoneRepository.findBySupplierIdAndId(supplierId, phoneId)
                .orElseThrow(() -> new RuntimeException("Supplier phone not found for supplier: " + supplierId));
        supplierPhoneRepository.delete(phone);
    }

}
