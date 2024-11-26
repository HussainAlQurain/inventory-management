package com.rayvision.inventory_management.service.impl;


import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.repository.CompanyRepository;
import com.rayvision.inventory_management.repository.UserRepository;
import com.rayvision.inventory_management.service.CompanyService;
import com.rayvision.inventory_management.service.UserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;


    CompanyServiceImpl(CompanyRepository companyRepository)
    {
        this.companyRepository = companyRepository;
    }

    @Override
    public Company save(Company company) {
        return companyRepository.save(company);
    }

    @Override
    public List<Company> findAll() {
        return StreamSupport.stream(companyRepository.findAll().spliterator(), false).collect(Collectors.toList());
    }

    @Override
    public Optional<Company> findOne(Long id) {
        return companyRepository.findById(id);
    }

    @Override
    public Company partialUpdate(Long id, Company company) {
        company.setId(id);
        return companyRepository.findById(id).map(existingCompany -> {
            Optional.ofNullable(company.getName()).ifPresent(existingCompany::setName);
            Optional.ofNullable(company.getTax_id()).ifPresent(existingCompany::setTax_id);
            Optional.ofNullable(company.getPhone()).ifPresent(existingCompany::setPhone);
            Optional.ofNullable(company.getMobile()).ifPresent(existingCompany::setMobile);
            Optional.ofNullable(company.getEmail()).ifPresent(existingCompany::setEmail);
            Optional.ofNullable(company.getState()).ifPresent(existingCompany::setState);
            Optional.ofNullable(company.getCity()).ifPresent(existingCompany::setCity);
            Optional.ofNullable(company.getAddress()).ifPresent(existingCompany::setAddress);
            Optional.ofNullable(company.getZip()).ifPresent(existingCompany::setZip);
            Optional.ofNullable(company.getAddPurchasedItemsToFavorites()).ifPresent(existingCompany::setAddPurchasedItemsToFavorites);
            Optional.ofNullable(company.getLogo()).ifPresent(existingCompany::setLogo);
            Optional.ofNullable(company.getAllowedInvoiceDeviation()).ifPresent(existingCompany::setAllowedInvoiceDeviation);
            Optional.ofNullable(company.getAccountingSoftware()).ifPresent(existingCompany::setAccountingSoftware);
            Optional.ofNullable(company.getExportDeliveryNotesAsBills()).ifPresent(existingCompany::setExportDeliveryNotesAsBills);
            return  companyRepository.save(existingCompany);
        }).orElseThrow(() -> new RuntimeException("Company doesn't exist"));
    }

    @Override
    public boolean isExists(Long id) {
        return companyRepository.existsById(id);
    }

    @Override
    public List<Company> findByUserId(Long userId) {
        return companyRepository.findCompaniesByUserId(userId);
    }

    @Override
    public List<Long> getCompanyIdsByUserId(Long userId) {
        return companyRepository.findCompaniesIdsByUserId(userId);
    }
}
