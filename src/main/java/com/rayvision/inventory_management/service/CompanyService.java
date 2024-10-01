package com.rayvision.inventory_management.service;


import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    CompanyService(CompanyRepository companyRepository)
    {
        this.companyRepository = companyRepository;
    }

    public Company createCompany(Company company)
    {
        return companyRepository.save(company);
    }
}
