package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

}
