package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.impl.CompanyMapperImpl;
import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.model.Users;
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

    private final CompanyService companyService;
    private final CompanyMapperImpl companyMapper;

    public CompanyController(CompanyService companyService, CompanyMapperImpl companyMapper) {
        this.companyService = companyService;
        this.companyMapper = companyMapper;
    }

    /* ------------------------------------------------------------------ */
    /* CREATE – POST /companies                                           */
    /* ------------------------------------------------------------------ */
    @PostMapping
    public ResponseEntity<CompanyDTO> createCompany(@RequestBody CompanyDTO dto) {
        Company entity  = companyMapper.mapFrom(dto);       // dto ➜ entity
        Company saved   = companyService.save(entity);
        return new ResponseEntity<>(companyMapper.mapTo(saved), HttpStatus.CREATED);
    }

    /* ------------------------------------------------------------------ */
    /* LIST  – GET /companies                                             */
    /* ------------------------------------------------------------------ */
    @GetMapping
    public ResponseEntity<List<CompanyDTO>> getCompanies() {
        List<CompanyDTO> list = companyService.findAll()
                .stream()
                .map(companyMapper::mapTo)
                .toList();
        return ResponseEntity.ok(list);
    }

    /* ------------------------------------------------------------------ */
    /* GET ONE – GET /companies/{id}                                      */
    /* ------------------------------------------------------------------ */
    @GetMapping("/{id}")
    public ResponseEntity<CompanyDTO> getCompanyById(@PathVariable Long id) {
        Optional<Company> opt = companyService.findOne(id);
        return opt.map(c -> ResponseEntity.ok(companyMapper.mapTo(c)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /* ------------------------------------------------------------------ */
    /* LIST BY USER – GET /companies/user/{userId}                     */
    /* ------------------------------------------------------------------ */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CompanyDTO>> getCompaniesByUserId(@PathVariable Long userId) {
        List<CompanyDTO> companies = companyService.findByUserId(userId);
        if(companies.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(companies);
    }

    @PostMapping("/{companyId}/users")
    public ResponseEntity<List<Users>> addUsersToCompany(@PathVariable Long companyId, @RequestBody List<Long> userIds) {
        List<Users> users = companyService.addUsersToCompany(companyId, userIds);
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/{companyId}/users/{userId}")
    public ResponseEntity<Void> deleteUserFromCompany(@PathVariable Long companyId, @PathVariable Long userId) {
        companyService.removeUserFromCompany(companyId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CompanyDTO> updateCompany(@PathVariable Long id, @RequestBody CompanyDTO patch) {
        Company entity = companyService.findOne(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        /* apply ONLY non‑null fields from patch dto */
        if (patch.getName()    != null) entity.setName   (patch.getName());
        if (patch.getTaxId()   != null) entity.setTaxId (patch.getTaxId());
        if (patch.getPhone()   != null) entity.setPhone (patch.getPhone());
        if (patch.getMobile()  != null) entity.setMobile(patch.getMobile());
        if (patch.getEmail()   != null) entity.setEmail (patch.getEmail());
        if (patch.getState()   != null) entity.setState (patch.getState());
        if (patch.getCity()    != null) entity.setCity  (patch.getCity());
        if (patch.getAddress() != null) entity.setAddress(patch.getAddress());
        if (patch.getZip()     != null) entity.setZip   (patch.getZip());
        if (patch.getAddPurchasedItemsToFavorites() != null)
            entity.setAddPurchasedItemsToFavorites(patch.getAddPurchasedItemsToFavorites());
        if (patch.getLogo()    != null) entity.setLogo  (patch.getLogo());
        if (patch.getAllowedInvoiceDeviation()  != null)
            entity.setAllowedInvoiceDeviation(patch.getAllowedInvoiceDeviation());
        if (patch.getAccountingSoftware() != null)
            entity.setAccountingSoftware(patch.getAccountingSoftware());
        if (patch.getExportDeliveryNotesAsBills() != null)
            entity.setExportDeliveryNotesAsBills(patch.getExportDeliveryNotesAsBills());

        Company saved = companyService.save(entity);
        return ResponseEntity.ok(companyMapper.mapTo(saved));
    }
}
