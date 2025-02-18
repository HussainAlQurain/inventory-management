package com.rayvision.inventory_management.controllers;


import com.rayvision.inventory_management.mappers.InventoryItemResponseMapper;
import com.rayvision.inventory_management.model.Supplier;
import com.rayvision.inventory_management.model.SupplierEmail;
import com.rayvision.inventory_management.model.SupplierPhone;
import com.rayvision.inventory_management.model.dto.*;
import com.rayvision.inventory_management.service.SupplierEmailService;
import com.rayvision.inventory_management.service.SupplierPhoneService;
import com.rayvision.inventory_management.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;
    private final SupplierEmailService supplierEmailService;
    private final SupplierPhoneService supplierPhoneService;
    private final InventoryItemResponseMapper mapper;

    @Autowired
    public SupplierController(
            SupplierService supplierService,
            SupplierEmailService supplierEmailService,
            SupplierPhoneService supplierPhoneService,
            InventoryItemResponseMapper inventoryItemResponseMapper
    ) {
        this.supplierService = supplierService;
        this.supplierEmailService = supplierEmailService;
        this.supplierPhoneService = supplierPhoneService;
        this.mapper = inventoryItemResponseMapper;
    }

    // 1) GET all Suppliers
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<SupplierResponseDTO>> getAllSuppliers(@PathVariable Long companyId) {
        List<Supplier> suppliers = supplierService.getAllSuppliers(companyId);
        List<SupplierResponseDTO> dtos = suppliers.stream()
                .map(mapper::toSupplierResponseDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // 2) GET single Supplier
    @GetMapping("/{id}/company/{companyId}")
    public ResponseEntity<SupplierResponseDTO> getSupplierById(
            @PathVariable Long id,
            @PathVariable Long companyId
    ) {
        Optional<Supplier> supplierOpt = supplierService.getSupplierById(companyId, id);
        if (supplierOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        SupplierResponseDTO dto = mapper.toSupplierResponseDTO(supplierOpt.get());
        return ResponseEntity.ok(dto);
    }

    // 3) CREATE new Supplier
    @PostMapping("/company/{companyId}")
    public ResponseEntity<SupplierResponseDTO> createSupplier(
            @PathVariable Long companyId,
            @RequestBody SupplierCreateDTO createDto
    ) {
        // Build or map a Supplier entity from createDto:
        Supplier newSupplier = new Supplier();
        newSupplier.setName(createDto.getName());
        newSupplier.setCustomerNumber(createDto.getCustomerNumber());
        newSupplier.setMinimumOrder(createDto.getMinimumOrder());
        newSupplier.setTaxId(createDto.getTaxId());
        newSupplier.setTaxRate(createDto.getTaxRate());
        newSupplier.setPaymentTerms(createDto.getPaymentTerms());
        newSupplier.setComments(createDto.getComments());
        newSupplier.setAddress(createDto.getAddress());
        newSupplier.setCity(createDto.getCity());
        newSupplier.setState(createDto.getState());
        newSupplier.setZip(createDto.getZip());
        newSupplier.setCcEmails(createDto.getCcEmails());

        Supplier saved = supplierService.save(companyId, newSupplier);
        SupplierResponseDTO resp = mapper.toSupplierResponseDTO(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    // 4) PARTIAL UPDATE existing Supplier
    @PatchMapping("/{id}/company/{companyId}")
    public ResponseEntity<SupplierResponseDTO> partialUpdateSupplier(
            @PathVariable Long id,
            @PathVariable Long companyId,
            @RequestBody SupplierPartialUpdateDTO patchDto
    ) {
        // Build a minimal Supplier entity with the ID
        Supplier patchEntity = new Supplier();
        patchEntity.setId(id);
        // Only set fields from patchDto that are not null
        if (patchDto.getName() != null) patchEntity.setName(patchDto.getName());
        if (patchDto.getCustomerNumber() != null) patchEntity.setCustomerNumber(patchDto.getCustomerNumber());
        if (patchDto.getMinimumOrder() != null) patchEntity.setMinimumOrder(patchDto.getMinimumOrder());
        if (patchDto.getTaxId() != null) patchEntity.setTaxId(patchDto.getTaxId());
        if (patchDto.getTaxRate() != null) patchEntity.setTaxRate(patchDto.getTaxRate());
        if (patchDto.getPaymentTerms() != null) patchEntity.setPaymentTerms(patchDto.getPaymentTerms());
        if (patchDto.getComments() != null) patchEntity.setComments(patchDto.getComments());
        if (patchDto.getAddress() != null) patchEntity.setAddress(patchDto.getAddress());
        if (patchDto.getCity() != null) patchEntity.setCity(patchDto.getCity());
        if (patchDto.getState() != null) patchEntity.setState(patchDto.getState());
        if (patchDto.getZip() != null) patchEntity.setZip(patchDto.getZip());
        if (patchDto.getCcEmails() != null) patchEntity.setCcEmails(patchDto.getCcEmails());

        Supplier updated = supplierService.partialUpdate(companyId, patchEntity);
        SupplierResponseDTO resp = mapper.toSupplierResponseDTO(updated);
        return ResponseEntity.ok(resp);
    }

    // 5) DELETE Supplier
    @DeleteMapping("/{id}/company/{companyId}")
    public ResponseEntity<Void> deleteSupplier(
            @PathVariable Long id,
            @PathVariable Long companyId
    ) {
        supplierService.deleteSupplierById(companyId, id);
        return ResponseEntity.noContent().build();
    }


    // ====== SUPPLIER EMAIL SUB-RESOURCE ======

    // 6) GET all Emails for a Supplier
    @GetMapping("/{supplierId}/company/{companyId}/emails")
    public ResponseEntity<List<SupplierEmailResponseDTO>> getSupplierEmails(
            @PathVariable Long supplierId,
            @PathVariable Long companyId
    ) {
        // Possibly validate the supplier first, or rely on service logic
        List<SupplierEmail> emails = supplierEmailService.getEmailsBySupplier(companyId, supplierId);
        List<SupplierEmailResponseDTO> dtos = emails.stream()
                .map(mapper::toSupplierEmailResponseDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // 7) CREATE a new SupplierEmail
    @PostMapping("/{supplierId}/company/{companyId}/emails")
    public ResponseEntity<SupplierEmailResponseDTO> createSupplierEmail(
            @PathVariable Long supplierId,
            @PathVariable Long companyId,
            @RequestBody SupplierEmailCreateDTO dto
    ) {
        // Build entity from dto
        SupplierEmail email = new SupplierEmail();
        email.setEmail(dto.getEmail());
        email.setDefault(dto.isDefault());

        SupplierEmail saved = supplierEmailService.saveEmail(companyId, supplierId, email);
        SupplierEmailResponseDTO resp = mapper.toSupplierEmailResponseDTO(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    // 8) PATCH or PUT for an existing SupplierEmail
    @PatchMapping("/{supplierId}/company/{companyId}/emails/{emailId}")
    public ResponseEntity<SupplierEmailResponseDTO> updateSupplierEmail(
            @PathVariable Long supplierId,
            @PathVariable Long companyId,
            @PathVariable Long emailId,
            @RequestBody SupplierEmailPartialUpdateDTO dto
    ) {
        // Build partial entity
        SupplierEmail email = new SupplierEmail();
        email.setId(emailId);
        if (dto.getEmail() != null) email.setEmail(dto.getEmail());
        // We always update isDefault, so:
        email.setDefault(dto.isDefault());

        SupplierEmail updated = supplierEmailService.updateEmail(companyId, supplierId, email);
        SupplierEmailResponseDTO resp = mapper.toSupplierEmailResponseDTO(updated);
        return ResponseEntity.ok(resp);
    }

    // 9) DELETE a SupplierEmail
    @DeleteMapping("/{supplierId}/company/{companyId}/emails/{emailId}")
    public ResponseEntity<Void> deleteSupplierEmail(
            @PathVariable Long supplierId,
            @PathVariable Long companyId,
            @PathVariable Long emailId
    ) {
        supplierEmailService.deleteEmail(companyId, supplierId, emailId);
        return ResponseEntity.noContent().build();
    }


    // ====== SUPPLIER PHONE SUB-RESOURCE ======

    // Similarly for phones:
    @GetMapping("/{supplierId}/company/{companyId}/phones")
    public ResponseEntity<List<SupplierPhoneResponseDTO>> getSupplierPhones(
            @PathVariable Long supplierId,
            @PathVariable Long companyId
    ) {
        List<SupplierPhone> phones = supplierPhoneService.getPhonesBySupplier(companyId, supplierId);
        List<SupplierPhoneResponseDTO> dtos = phones.stream()
                .map(mapper::toSupplierPhoneResponseDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{supplierId}/company/{companyId}/phones")
    public ResponseEntity<SupplierPhoneResponseDTO> createSupplierPhone(
            @PathVariable Long supplierId,
            @PathVariable Long companyId,
            @RequestBody SupplierPhoneCreateDTO dto
    ) {
        SupplierPhone phone = new SupplierPhone();
        phone.setPhoneNumber(dto.getPhoneNumber());
        phone.setDefault(dto.isDefault());

        SupplierPhone saved = supplierPhoneService.savePhone(companyId, supplierId, phone);
        SupplierPhoneResponseDTO resp = mapper.toSupplierPhoneResponseDTO(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PatchMapping("/{supplierId}/company/{companyId}/phones/{phoneId}")
    public ResponseEntity<SupplierPhoneResponseDTO> updateSupplierPhone(
            @PathVariable Long supplierId,
            @PathVariable Long companyId,
            @PathVariable Long phoneId,
            @RequestBody SupplierPhonePartialUpdateDTO dto
    ) {
        SupplierPhone phone = new SupplierPhone();
        phone.setId(phoneId);
        if (dto.getPhoneNumber() != null) phone.setPhoneNumber(dto.getPhoneNumber());
        phone.setDefault(dto.isDefault());

        SupplierPhone updated = supplierPhoneService.updatePhone(companyId, supplierId, phone);
        SupplierPhoneResponseDTO resp = mapper.toSupplierPhoneResponseDTO(updated);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{supplierId}/company/{companyId}/phones/{phoneId}")
    public ResponseEntity<Void> deleteSupplierPhone(
            @PathVariable Long supplierId,
            @PathVariable Long companyId,
            @PathVariable Long phoneId
    ) {
        supplierPhoneService.deletePhone(companyId, supplierId, phoneId);
        return ResponseEntity.noContent().build();
    }
}
