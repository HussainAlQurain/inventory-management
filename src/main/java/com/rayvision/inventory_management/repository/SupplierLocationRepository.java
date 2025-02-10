package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.SupplierLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierLocationRepository extends JpaRepository<SupplierLocation, Long> {
    List<SupplierLocation> findBySupplierId(Long supplierId);
    Optional<SupplierLocation> findBySupplierIdAndId(Long supplierId, Long id);
    Optional<SupplierLocation> findBySupplierIdAndLocationId(Long supplierId, Long locationId);
}
