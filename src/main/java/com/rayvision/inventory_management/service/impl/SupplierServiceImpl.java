package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.repository.CategoryRepository;
import com.rayvision.inventory_management.repository.CompanyRepository;
import com.rayvision.inventory_management.repository.LocationRepository;
import com.rayvision.inventory_management.repository.SupplierRepository;
import com.rayvision.inventory_management.service.SupplierService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SupplierServiceImpl implements SupplierService {
    private final SupplierRepository supplierRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;


    public SupplierServiceImpl(SupplierRepository supplierRepository, CompanyRepository companyRepository, CategoryRepository categoryRepository, LocationRepository locationRepository) {
        this.supplierRepository = supplierRepository;
        this.companyRepository = companyRepository;
        this.categoryRepository = categoryRepository;
        this.locationRepository = locationRepository;
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
        // (1) Link the Supplier to the Company
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Invalid Company ID: " + companyId));
        supplier.setCompany(company);

        // (2) If defaultCategory != null & has an ID, fetch from DB
        if (supplier.getDefaultCategory() != null && supplier.getDefaultCategory().getId() != null) {
            Long catId = supplier.getDefaultCategory().getId();
            Category cat = categoryRepository.findByCompanyIdAndId(companyId, catId)
                    .orElseThrow(() -> new RuntimeException("Category not found for id: " + catId));
            supplier.setDefaultCategory(cat);
        }

        // (3) If we have authorizedBuyerIds, build SupplierLocation bridging
        if (supplier.getAuthorizedBuyerIds() != null && !supplier.getAuthorizedBuyerIds().isEmpty()) {
            for (Long locId : supplier.getAuthorizedBuyerIds()) {
                Location loc = locationRepository.findByCompanyIdAndId(companyId, locId)
                        .orElseThrow(() -> new RuntimeException("Location not found for id: " + locId));

                SupplierLocation sl = new SupplierLocation();
                sl.setSupplier(supplier);
                sl.setLocation(loc);

                // Because supplier.authorizedBuyers is a Set<SupplierLocation> mapped by "supplier"
                supplier.getAuthorizedBuyers().add(sl);
            }
        }

        // (4) If we have phones/emails, they should already be linked
        //     (phone.setSupplier(supplier)) in the mapper or an after-mapping step.
        //     Just confirm we have CascadeType.ALL or do the setSupplier if needed.

        // (5) Finally, persist
        return supplierRepository.save(supplier);
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

    @Override
    public List<Supplier> searchSuppliers(Long companyId, String searchTerm) {
        return supplierRepository.findByCompanyIdAndNameContaining(companyId, searchTerm);
    }

}
