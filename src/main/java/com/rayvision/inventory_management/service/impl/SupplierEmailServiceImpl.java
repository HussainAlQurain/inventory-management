package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.Supplier;
import com.rayvision.inventory_management.model.SupplierEmail;
import com.rayvision.inventory_management.repository.SupplierEmailRepository;
import com.rayvision.inventory_management.repository.SupplierRepository;
import com.rayvision.inventory_management.service.SupplierEmailService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SupplierEmailServiceImpl implements SupplierEmailService {
    private final SupplierEmailRepository supplierEmailRepository;
    private final SupplierRepository supplierRepository;

    public SupplierEmailServiceImpl(SupplierEmailRepository supplierEmailRepository, SupplierRepository supplierRepository) {
        this.supplierEmailRepository = supplierEmailRepository;
        this.supplierRepository = supplierRepository;
    }

    @Override
    public List<SupplierEmail> getEmailsBySupplier(Long companyId, Long supplierId) {
        // You might also check that the supplier belongs to the company
        return supplierEmailRepository.findBySupplierId(supplierId);
    }

    @Override
    public Optional<SupplierEmail> getEmailById(Long companyId, Long supplierId, Long emailId) {
        return supplierEmailRepository.findBySupplierIdAndId(supplierId, emailId);
    }

    @Override
    public SupplierEmail saveEmail(Long companyId, Long supplierId, SupplierEmail email) {
        // Ensure the supplier exists and belongs to the company:
        Supplier supplier = supplierRepository.findByCompanyIdAndId(companyId, supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found for company: " + companyId));
        email.setSupplier(supplier);
        return supplierEmailRepository.save(email);
    }

    @Override
    public SupplierEmail updateEmail(Long companyId, Long supplierId, SupplierEmail email) {
        return supplierEmailRepository.findBySupplierIdAndId(supplierId, email.getId())
                .map(existingEmail -> {
                    if (email.getEmail() != null) existingEmail.setEmail(email.getEmail());
                    existingEmail.setDefault(email.isDefault());
                    return supplierEmailRepository.save(existingEmail);
                }).orElseThrow(() -> new RuntimeException("Supplier email not found"));
    }

    @Override
    public void deleteEmail(Long companyId, Long supplierId, Long emailId) {
        SupplierEmail email = supplierEmailRepository.findBySupplierIdAndId(supplierId, emailId)
                .orElseThrow(() -> new RuntimeException("Supplier email not found"));
        supplierEmailRepository.delete(email);
    }

}
