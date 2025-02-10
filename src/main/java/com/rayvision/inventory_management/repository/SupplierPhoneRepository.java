package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.SupplierPhone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierPhoneRepository extends JpaRepository<SupplierPhone, Long> {
    List<SupplierPhone> findBySupplierId(Long supplierId);
    Optional<SupplierPhone> findBySupplierIdAndId(Long supplierId, Long id);
}
