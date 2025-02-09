package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByCompanyId(Long companyId);
    Optional<Supplier> findByCompanyIdAndId(Long companyId, Long id);
}
