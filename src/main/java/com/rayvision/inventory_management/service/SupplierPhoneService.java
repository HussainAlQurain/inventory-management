package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.SupplierPhone;

import java.util.List;
import java.util.Optional;

public interface SupplierPhoneService {
    List<SupplierPhone> getPhonesBySupplier(Long companyId, Long supplierId);
    Optional<SupplierPhone> getPhoneById(Long companyId, Long supplierId, Long phoneId);
    SupplierPhone savePhone(Long companyId, Long supplierId, SupplierPhone phone);
    SupplierPhone updatePhone(Long companyId, Long supplierId, SupplierPhone phone);
    void deletePhone(Long companyId, Long supplierId, Long phoneId);
}
