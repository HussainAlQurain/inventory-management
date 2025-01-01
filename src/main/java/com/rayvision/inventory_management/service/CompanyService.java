package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.model.dto.CompanyDTO;

import java.util.List;
import java.util.Optional;

public interface CompanyService {
    Company save(Company company);
    List<Company> findAll();
    Optional<Company> findOne(Long id);
    List<Long> getCompanyIdsByUserId(Long userId);
    Company partialUpdate(Long id, Company company);
    boolean isExists(Long id);

    List<CompanyDTO> findByUserId(Long userId);
}
