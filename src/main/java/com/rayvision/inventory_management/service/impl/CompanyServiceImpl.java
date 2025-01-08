package com.rayvision.inventory_management.service.impl;


import com.rayvision.inventory_management.mappers.Mapper;
import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.model.CompanyUser;
import com.rayvision.inventory_management.model.Users;
import com.rayvision.inventory_management.model.dto.CompanyDTO;
import com.rayvision.inventory_management.repository.CompanyRepository;
import com.rayvision.inventory_management.repository.CompanyUserRepository;
import com.rayvision.inventory_management.repository.UserRepository;
import com.rayvision.inventory_management.service.CompanyService;
import com.rayvision.inventory_management.service.UserService;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.Option;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    private Mapper<Company, CompanyDTO> companyMapper;

    private CompanyUserRepository companyUserRepository;

    public CompanyServiceImpl(CompanyRepository companyRepository, Mapper<Company, CompanyDTO> companyMapper, CompanyUserRepository companyUserRepository, UserRepository userRepository) {
        this.companyRepository = companyRepository;
        this.companyMapper = companyMapper;
        this.companyUserRepository = companyUserRepository;
        this.userRepository = userRepository;
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
            Optional.ofNullable(company.getTaxId()).ifPresent(existingCompany::setTaxId);
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
            return companyRepository.save(existingCompany);
        }).orElseThrow(() -> new RuntimeException("Company doesn't exist"));
    }

    @Override
    public boolean isExists(Long id) {
        return companyRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public List<CompanyDTO> findByUserId(Long userId) {
        List<Long> companyIds = companyUserRepository.findCompanyIdsByUserId(userId);
        List<Company> companies = companyRepository.findAllById(companyIds);
        return companies.stream()
                .map(companyMapper::mapTo)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void delete(Long id) {
        companyRepository.deleteById(id);
    }

    @Transactional
    @Override
    public List<Users> addUsersToCompany(Long companyId, List<Long> userIds) {
        // Fetch the company
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company doesn't exist. ID: " + companyId));

        // Fetch users by IDs
        List<Users> users = userRepository.findAllById(userIds);
        if (users.isEmpty()) {
            throw new RuntimeException("No valid users found for the given IDs.");
        }

        // Find invalid IDs (requested but not found in DB)
        Set<Long> foundUserIds = users.stream().map(Users::getId).collect(Collectors.toSet());
        List<Long> invalidIds = userIds.stream()
                .filter(id -> !foundUserIds.contains(id))
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new RuntimeException("The following user IDs are invalid: " + invalidIds);
        }

        // Filter out users already associated with the company
        Set<Long> existingUserIds = company.getCompanyUsers()
                .stream()
                .map(companyUser -> companyUser.getUser().getId())
                .collect(Collectors.toSet());

        List<CompanyUser> newCompanyUsers = users.stream()
                .filter(user -> !existingUserIds.contains(user.getId())) // Skip existing users
                .map(user -> CompanyUser.builder().company(company).user(user).build())
                .toList();

        // Save only new CompanyUser associations
        if (!newCompanyUsers.isEmpty()) {
            companyUserRepository.saveAll(newCompanyUsers);
        }

        // Fetch and return the newly added users
        return newCompanyUsers.stream()
                .map(CompanyUser::getUser)
                .toList();
    }

    @Override
    public void removeUserFromCompany(Long companyId, Long userId) {
        Optional<CompanyUser> companyUser = companyUserRepository.findByCompanyIdAndUserId(companyId, userId);
        if (companyUser.isPresent()) {
            companyUserRepository.delete(companyUser.get());
        }
        else {
            throw new RuntimeException("User is not associated with the company: " + companyId);
        }
    }
}
