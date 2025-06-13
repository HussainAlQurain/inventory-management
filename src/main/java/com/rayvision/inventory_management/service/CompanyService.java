package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.model.dto.CompanyDTO;

import java.util.List;
import java.util.Optional;

public interface CompanyService {
    Company save(Company company);
    List<Company> findAll();
    Optional<Company> findOne(Long id);
    Company partialUpdate(Long id, Company company);
    boolean isExists(Long id);
    List<CompanyDTO> findByUserId(Long userId);
    void delete(Long id);
    List<Users> addUsersToCompany(Long companyId, List<Long> users);
    void removeUserFromCompany(Long companyId, Long userId);
}
