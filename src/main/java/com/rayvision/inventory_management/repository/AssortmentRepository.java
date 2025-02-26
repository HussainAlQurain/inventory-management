package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Assortment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssortmentRepository extends JpaRepository<Assortment, Long> {
    // e.g. find by company
    List<Assortment> findByCompanyId(Long companyId);
}
