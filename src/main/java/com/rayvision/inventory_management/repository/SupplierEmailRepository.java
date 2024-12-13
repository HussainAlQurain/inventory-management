package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.SupplierEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierEmailRepository extends JpaRepository<SupplierEmail, Long> {
}
