package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/companies")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @PostMapping
    public Company createCompany(@RequestBody Company company)
    {
        return companyService.createCompany(company);
    }
}
