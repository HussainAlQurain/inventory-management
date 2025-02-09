package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.Supplier;
import com.rayvision.inventory_management.repository.CompanyRepository;
import com.rayvision.inventory_management.repository.SupplierRepository;
import com.rayvision.inventory_management.service.SupplierService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SupplierServiceImpl implements SupplierService {
    private final SupplierRepository supplierRepository;
    private final CompanyRepository companyRepository;

    public SupplierServiceImpl(SupplierRepository supplierRepository, CompanyRepository companyRepository) {
        this.supplierRepository = supplierRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    public List<Supplier> getAllSuppliers(Long companyId) {
        return supplierRepository.findByCompanyId(companyId);
    }

    @Override
    public Optional<Supplier> getSupplierById(Long companyId, Long id) {
        return supplierRepository.findByCompanyIdAndId(companyId, id);
    }

    @Override
    public Supplier save(Long companyId, Supplier supplier) {
        return companyRepository.findById(companyId).map(company -> {
            supplier.setCompany(company);
            return supplierRepository.save(supplier);
        }).orElseThrow(() -> new RuntimeException("Invalid Company ID: " + companyId));
    }

    @Override
    public Supplier update(Long companyId, Supplier supplier) {
        return supplierRepository.findByCompanyIdAndId(companyId, supplier.getId())
                .map(existing -> {
                    existing.setName(supplier.getName());
                    existing.setCustomerNumber(supplier.getCustomerNumber());
                    existing.setMinimumOrder(supplier.getMinimumOrder());
                    existing.setTaxId(supplier.getTaxId());
                    existing.setTaxRate(supplier.getTaxRate());
                    existing.setPaymentTerms(supplier.getPaymentTerms());
                    existing.setComments(supplier.getComments());
                    existing.setAddress(supplier.getAddress());
                    existing.setCity(supplier.getCity());
                    existing.setState(supplier.getState());
                    existing.setZip(supplier.getZip());
                    existing.setCcEmails(supplier.getCcEmails());
                    // Update other fields or associations as needed
                    return supplierRepository.save(existing);
                }).orElseThrow(() -> new RuntimeException("Supplier not found for company: " + companyId));
    }

    @Override
    public Supplier partialUpdate(Long companyId, Supplier supplier) {
        return supplierRepository.findByCompanyIdAndId(companyId, supplier.getId()).map(existingSupplier -> {
            Optional.ofNullable(supplier.getName()).ifPresent(existingSupplier::setName);
            Optional.ofNullable(supplier.getCustomerNumber()).ifPresent(existingSupplier::setCustomerNumber);
            Optional.ofNullable(supplier.getMinimumOrder()).ifPresent(existingSupplier::setMinimumOrder);
            Optional.ofNullable(supplier.getTaxId()).ifPresent(existingSupplier::setTaxId);
            Optional.ofNullable(supplier.getTaxRate()).ifPresent(existingSupplier::setTaxRate);
            Optional.ofNullable(supplier.getPaymentTerms()).ifPresent(existingSupplier::setPaymentTerms);
            Optional.ofNullable(supplier.getComments()).ifPresent(existingSupplier::setComments);
            Optional.ofNullable(supplier.getAddress()).ifPresent(existingSupplier::setAddress);
            Optional.ofNullable(supplier.getCity()).ifPresent(existingSupplier::setCity);
            Optional.ofNullable(supplier.getState()).ifPresent(existingSupplier::setState);
            Optional.ofNullable(supplier.getZip()).ifPresent(existingSupplier::setZip);
            Optional.ofNullable(supplier.getCcEmails()).ifPresent(existingSupplier::setCcEmails);
            // Update other nested fields if necessary.
            return supplierRepository.save(existingSupplier);
        }).orElseThrow(() -> new RuntimeException("Could not find supplier with id: " + supplier.getId()));
    }

    @Override
    public void deleteSupplierById(Long companyId, Long id) {
        Supplier supplier = supplierRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new RuntimeException("Supplier not found for company: " + companyId));
        supplierRepository.delete(supplier);
    }


}
