package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.model.dto.CompanyDTO;
import com.rayvision.inventory_management.service.CompanyService;
import com.rayvision.inventory_management.service.impl.CompanyServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/companies")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @PostMapping
    public Company createCompany(@RequestBody Company company)
    {
        return companyService.save(company);
    }

    @GetMapping
    public List<Company> getCompanies()
    {
        return companyService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Company> getCompanyById(@PathVariable Long id)
    {
        Optional<Company> foundCompany = companyService.findOne(id);
        return foundCompany.map(company -> {
            return new ResponseEntity<>(company, HttpStatus.OK);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CompanyDTO>> getCompaniesByUserId(@PathVariable Long userId) {
        List<CompanyDTO> companies = companyService.findByUserId(userId);
        if(companies.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(companies);
    }
}
