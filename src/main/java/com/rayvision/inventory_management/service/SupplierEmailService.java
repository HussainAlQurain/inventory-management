package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.SupplierEmail;

import java.util.List;
import java.util.Optional;

public interface SupplierEmailService {
    List<SupplierEmail> getEmailsBySupplier(Long companyId, Long supplierId);
    Optional<SupplierEmail> getEmailById(Long companyId, Long supplierId, Long emailId);
    SupplierEmail saveEmail(Long companyId, Long supplierId, SupplierEmail email);
    SupplierEmail updateEmail(Long companyId, Long supplierId, SupplierEmail email);
    void deleteEmail(Long companyId, Long supplierId, Long emailId);
}
