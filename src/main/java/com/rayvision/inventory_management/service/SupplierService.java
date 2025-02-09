package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.Supplier;

import java.util.List;
import java.util.Optional;

public interface SupplierService {
    List<Supplier> getAllSuppliers(Long companyId);
    Optional<Supplier> getSupplierById(Long companyId, Long id);
    Supplier save(Long companyId, Supplier supplier);
    Supplier update(Long companyId, Supplier supplier);
    Supplier partialUpdate(Long companyId, Supplier supplier);
    void deleteSupplierById(Long companyId, Long id);
}
