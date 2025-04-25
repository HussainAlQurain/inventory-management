package com.rayvision.inventory_management.controllers;


import com.rayvision.inventory_management.exceptions.ResourceNotFoundException;
import com.rayvision.inventory_management.mappers.InventoryItemResponseMapper;
import com.rayvision.inventory_management.mappers.SupplierMapper;
import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.Supplier;
import com.rayvision.inventory_management.model.SupplierEmail;
import com.rayvision.inventory_management.model.SupplierPhone;
import com.rayvision.inventory_management.model.dto.*;
import com.rayvision.inventory_management.service.LocationService;
import com.rayvision.inventory_management.service.SupplierEmailService;
import com.rayvision.inventory_management.service.SupplierPhoneService;
import com.rayvision.inventory_management.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;
    private final SupplierEmailService supplierEmailService;
    private final SupplierPhoneService supplierPhoneService;
    private final SupplierMapper supplierMapper; // <--- The new dedicated mapper
    private final LocationService locationService;

    @Autowired
    public SupplierController(
            SupplierService supplierService,
            SupplierEmailService supplierEmailService,
            SupplierPhoneService supplierPhoneService,
            SupplierMapper supplierMapper,
            LocationService locationService
    ) {
        this.supplierService = supplierService;
        this.supplierEmailService = supplierEmailService;
        this.supplierPhoneService = supplierPhoneService;
        this.supplierMapper = supplierMapper;
        this.locationService = locationService;
    }

    // 1) GET all Suppliers
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<SupplierResponseDTO>> getAllSuppliers(@PathVariable Long companyId) {
        List<Supplier> suppliers = supplierService.getAllSuppliers(companyId);
        List<SupplierResponseDTO> dtos = suppliers.stream()
                .map(supplierMapper::toSupplierResponseDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * GET all Suppliers for a company with optional search by name.
     */
    @GetMapping("/company/{companyId}/search")
    public ResponseEntity<List<SupplierResponseDTO>> searchSuppliers(
            @PathVariable Long companyId,
            @RequestParam(name = "search", defaultValue = "") String searchTerm) {
        List<Supplier> suppliers = supplierService.searchSuppliers(companyId, searchTerm);
        List<SupplierResponseDTO> dtos = suppliers.stream()
                .map(supplierMapper::toSupplierResponseDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * GET paginated suppliers for a company with search, sorting and filtering
     * @param companyId The company ID
     * @param page Page number (zero-based)
     * @param size Page size
     * @param sort Sort field and direction (e.g. "name,asc")
     * @param search Search term for supplier name
     * @return Paginated list of suppliers
     */
    @GetMapping("/company/{companyId}/paginated")
    public ResponseEntity<PageResponseDTO<SupplierResponseDTO>> getPaginatedSuppliers(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "") String search) {
        
        // Create sorting if provided
        Sort sorting = Sort.unsorted();
        if (sort != null && !sort.isEmpty()) {
            String[] sortParams = sort.split(",");
            String sortField = sortParams[0];
            Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            sorting = Sort.by(direction, sortField);
        } else {
            // Default sort by name ascending
            sorting = Sort.by(Sort.Direction.ASC, "name");
        }
        
        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<Supplier> suppliersPage = supplierService.findPaginatedSuppliers(companyId, search, pageable);
        
        // Convert entities to DTOs
        Page<SupplierResponseDTO> dtoPage = suppliersPage.map(supplierMapper::toSupplierResponseDTO);
        
        PageResponseDTO<SupplierResponseDTO> response = new PageResponseDTO<>(
                dtoPage.getContent(),
                dtoPage.getTotalElements(),
                dtoPage.getTotalPages(),
                dtoPage.getNumber(),
                dtoPage.getSize(),
                dtoPage.hasNext(),
                dtoPage.hasPrevious()
        );
        
        return ResponseEntity.ok(response);
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
        SupplierResponseDTO dto = supplierMapper.toSupplierResponseDTO(supplierOpt.get());
        return ResponseEntity.ok(dto);
    }

    // 3) CREATE new Supplier
    @PostMapping("/company/{companyId}")
    public ResponseEntity<SupplierResponseDTO> createSupplier(
            @PathVariable Long companyId,
            @RequestBody SupplierCreateDTO createDto
    ) {
        // A) Map the DTO to a new Supplier
        Supplier newSupplier = supplierMapper.fromSupplierCreateDTO(createDto);

        // Ensure all emails have supplier reference set
        if (newSupplier.getOrderEmails() != null) {
            newSupplier.getOrderEmails().forEach(email -> email.setSupplier(newSupplier));
        }

        // Ensure all phones have supplier reference set
        if (newSupplier.getOrderPhones() != null) {
            newSupplier.getOrderPhones().forEach(phone -> phone.setSupplier(newSupplier));
        }

        // B) Call service (which sets the company, fetches defaultCategory if needed, etc.)
        Supplier saved = supplierService.save(companyId, newSupplier);

        // C) Convert to response
        SupplierResponseDTO resp = supplierMapper.toSupplierResponseDTO(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    // 4) PARTIAL UPDATE existing Supplier
    //    (still manual or you can create a "updateSupplierFromPartialDTO(...)" method in SupplierMapper)
    @PatchMapping("/{id}/company/{companyId}")
    public ResponseEntity<SupplierResponseDTO> partialUpdateSupplier(
            @PathVariable Long id,
            @PathVariable Long companyId,
            @Valid @RequestBody SupplierPartialUpdateDTO patchDto) {

        // 1) find existing from DB
        Supplier existing = supplierService.findByCompanyIdAndId(companyId, id).orElseThrow(() -> new ResourceNotFoundException("Supplier with id " + id + " not found"));
        // or something like: supplierService.getSupplier(companyId, id)

        // 2) update top-level fields
        if (patchDto.getName() != null) existing.setName(patchDto.getName());
        if (patchDto.getCustomerNumber() != null) existing.setCustomerNumber(patchDto.getCustomerNumber());
        if (patchDto.getMinimumOrder() != null) existing.setMinimumOrder(patchDto.getMinimumOrder());
        if (patchDto.getTaxId() != null) existing.setTaxId(patchDto.getTaxId());
        if (patchDto.getTaxRate() != null) existing.setTaxRate(patchDto.getTaxRate());
        if (patchDto.getPaymentTerms() != null) existing.setPaymentTerms(patchDto.getPaymentTerms());
        if (patchDto.getComments() != null) existing.setComments(patchDto.getComments());
        if (patchDto.getAddress() != null) existing.setAddress(patchDto.getAddress());
        if (patchDto.getCity() != null) existing.setCity(patchDto.getCity());
        if (patchDto.getState() != null) existing.setState(patchDto.getState());
        if (patchDto.getZip() != null) existing.setZip(patchDto.getZip());
        if (patchDto.getCcEmails() != null) existing.setCcEmails(patchDto.getCcEmails());
        // etc.

        // 3) Merge emails if they are provided
        if (patchDto.getEmails() != null) {
            // the user is passing an array of e.g. SupplierEmailDTO with some ID or null
            mergeSupplierEmails(existing, patchDto.getEmails());
        }

        // 4) Merge phones if they are provided
        if (patchDto.getPhones() != null) {
            mergeSupplierPhones(existing, patchDto.getPhones());
        }

        // 5) Now we pass the updated entity to the service to save
        Supplier updated = supplierService.partialUpdate(companyId, existing);

        // 6) Convert to response
        SupplierResponseDTO resp = supplierMapper.toSupplierResponseDTO(updated);
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
        List<SupplierEmail> emails = supplierEmailService.getEmailsBySupplier(companyId, supplierId);
        List<SupplierEmailResponseDTO> dtos = emails.stream()
                // you can use supplierMapper.toSupplierEmailResponseDTO if you prefer
                .map(supplierMapper::toSupplierEmailResponseDTO)
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
        SupplierEmail email = new SupplierEmail();
        email.setEmail(dto.getEmail());
        email.setDefault(dto.isDefault());
        if (dto.getLocationId() != null) {
            Location location = locationService.findByIdAndCompanyId(dto.getLocationId(), companyId).orElseThrow(() -> new ResourceNotFoundException("Location not found"));
            email.setLocation(location);
        }

        SupplierEmail saved = supplierEmailService.saveEmail(companyId, supplierId, email);
        SupplierEmailResponseDTO resp = supplierMapper.toSupplierEmailResponseDTO(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    // 8) PATCH for an existing SupplierEmail
    @PatchMapping("/{supplierId}/company/{companyId}/emails/{emailId}")
    public ResponseEntity<SupplierEmailResponseDTO> updateSupplierEmail(
            @PathVariable Long supplierId,
            @PathVariable Long companyId,
            @PathVariable Long emailId,
            @RequestBody SupplierEmailPartialUpdateDTO dto
    ) {
        SupplierEmail email = new SupplierEmail();
        email.setId(emailId);
        if (dto.getEmail() != null) email.setEmail(dto.getEmail());
        // We always update isDefault
        email.setDefault(dto.isDefault());

        SupplierEmail updated = supplierEmailService.updateEmail(companyId, supplierId, email);
        SupplierEmailResponseDTO resp = supplierMapper.toSupplierEmailResponseDTO(updated);
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

    @GetMapping("/{supplierId}/company/{companyId}/phones")
    public ResponseEntity<List<SupplierPhoneResponseDTO>> getSupplierPhones(
            @PathVariable Long supplierId,
            @PathVariable Long companyId
    ) {
        List<SupplierPhone> phones = supplierPhoneService.getPhonesBySupplier(companyId, supplierId);
        List<SupplierPhoneResponseDTO> dtos = phones.stream()
                .map(supplierMapper::toSupplierPhoneResponseDTO)
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
        if (dto.getLocationId() != null) {
            Location location = locationService.findByIdAndCompanyId(dto.getLocationId(), companyId).orElseThrow(() -> new ResourceNotFoundException("Location not found"));
            phone.setLocation(location);
        }

        SupplierPhone saved = supplierPhoneService.savePhone(companyId, supplierId, phone);
        SupplierPhoneResponseDTO resp = supplierMapper.toSupplierPhoneResponseDTO(saved);
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
        SupplierPhoneResponseDTO resp = supplierMapper.toSupplierPhoneResponseDTO(updated);
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

    private void mergeSupplierEmails(Supplier existing, List<SupplierEmailDTO> newEmails) {
        // 1) map existing set by ID
        Map<Long, SupplierEmail> existingMap = new HashMap<>();
        for (SupplierEmail e : existing.getOrderEmails()) {
            existingMap.put(e.getId(), e);
        }

        // 2) build a new set that will replace the old set
        Set<SupplierEmail> mergedSet = new HashSet<>();

        for (SupplierEmailDTO dto : newEmails) {
            // if dto.id != null => see if we have an existing
            if (dto.getId() != null && existingMap.containsKey(dto.getId())) {
                // update the existing record
                SupplierEmail existingEmail = existingMap.get(dto.getId());
                existingEmail.setEmail(dto.getEmail());
                existingEmail.setDefault(dto.isDefault());
                if (dto.getLocationId() != null) {
                    // find or set the location object
                    Location loc = locationService.findOne(dto.getLocationId())
                            .orElseThrow(() -> new RuntimeException("Location not found " + dto.getLocationId()));
                    existingEmail.setLocation(loc);
                } else {
                    existingEmail.setLocation(null);
                }
                mergedSet.add(existingEmail);
            } else {
                // create a new SupplierEmail
                SupplierEmail newEmail = new SupplierEmail();
                newEmail.setEmail(dto.getEmail());
                newEmail.setDefault(dto.isDefault());
                newEmail.setSupplier(existing);   // link back
                if (dto.getLocationId() != null) {
                    Location loc = locationService.findOne(dto.getLocationId())
                            .orElseThrow(() -> new RuntimeException("Location not found " + dto.getLocationId()));
                    newEmail.setLocation(loc);
                }
                mergedSet.add(newEmail);
            }
        }

        // 3) set the new set on the supplier
        // if you want to remove old emails that were not provided,
        // this approach effectively does that by building a new set
        existing.getOrderEmails().clear();
        existing.getOrderEmails().addAll(mergedSet);
    }

    private void mergeSupplierPhones(Supplier existing, List<SupplierPhoneDTO> newPhones) {
        // 1) Map existing phone records by ID
        Map<Long, SupplierPhone> existingMap = new HashMap<>();
        for (SupplierPhone p : existing.getOrderPhones()) {
            existingMap.put(p.getId(), p);
        }

        // 2) Build a new set that will replace the old set
        Set<SupplierPhone> mergedSet = new HashSet<>();

        for (SupplierPhoneDTO dto : newPhones) {
            // If the user-provided DTO has an ID that matches an existing phone, we update
            if (dto.getId() != null && existingMap.containsKey(dto.getId())) {
                SupplierPhone existingPhone = existingMap.get(dto.getId());
                // update fields
                existingPhone.setPhoneNumber(dto.getPhoneNumber());
                existingPhone.setDefault(dto.isDefault());
                if (dto.getLocationId() != null) {
                    Location loc = locationService.findOne(dto.getLocationId())
                            .orElseThrow(() -> new RuntimeException("Location not found: " + dto.getLocationId()));
                    existingPhone.setLocation(loc);
                } else {
                    existingPhone.setLocation(null);
                }
                mergedSet.add(existingPhone);

            } else {
                // Create a brand-new phone
                SupplierPhone newPhone = new SupplierPhone();
                newPhone.setPhoneNumber(dto.getPhoneNumber());
                newPhone.setDefault(dto.isDefault());
                newPhone.setSupplier(existing);  // link to the same Supplier
                if (dto.getLocationId() != null) {
                    Location loc = locationService.findOne(dto.getLocationId())
                            .orElseThrow(() -> new RuntimeException("Location not found: " + dto.getLocationId()));
                    newPhone.setLocation(loc);
                }
                mergedSet.add(newPhone);
            }
        }

        // 3) Clear the old set, then add the merged set
        existing.getOrderPhones().clear();
        existing.getOrderPhones().addAll(mergedSet);
    }

}
