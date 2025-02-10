package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.SupplierEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierEmailRepository extends JpaRepository<SupplierEmail, Long> {
    List<SupplierEmail> findBySupplierId(Long supplierId);
    Optional<SupplierEmail> findBySupplierIdAndId(Long supplierId, Long id);
}
