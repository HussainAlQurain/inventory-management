package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/companies")
public class CompanyController {

    private List<Company> companies = new ArrayList<Company>(100);

    public CompanyController()
    {
        for(int i = 0; i < 100; i++)
        {
            companies.add(Company.builder().id((long)i).name(String.valueOf((char) i)).build());
        }
    }

    @Autowired
    private CompanyService companyService;

    @PostMapping
    public Company createCompany(@RequestBody Company company)
    {
        return companyService.createCompany(company);
    }

    @GetMapping
    public List<Company> getCompanies()
    {
        // To be updated to get companies from database
        return companies;
    }

    @GetMapping("/{id}")
    public Company getCompanyById(@PathVariable Long id)
    {
        return companies.stream().filter(
                company -> company.getId().equals(id)
        ).findFirst().orElse(null);
    }
}
