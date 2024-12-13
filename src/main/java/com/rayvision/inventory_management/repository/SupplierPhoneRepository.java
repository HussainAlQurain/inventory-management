package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.SupplierPhone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierPhoneRepository extends JpaRepository<SupplierPhone, Long> {
}
