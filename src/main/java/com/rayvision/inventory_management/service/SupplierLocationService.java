package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.SupplierLocation;

import java.util.List;
import java.util.Optional;

public interface SupplierLocationService {
    List<SupplierLocation> getLocationsBySupplier(Long companyId, Long supplierId);
    Optional<SupplierLocation> getSupplierLocationById(Long companyId, Long supplierId, Long locationId);
    SupplierLocation saveSupplierLocation(Long companyId, Long supplierId, SupplierLocation supplierLocation);
    void deleteSupplierLocation(Long companyId, Long supplierId, Long locationId);
}
