package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SupplierService {
    List<Supplier> getAllSuppliers(Long companyId);
    Optional<Supplier> getSupplierById(Long companyId, Long id);
    Supplier save(Long companyId, Supplier supplier);
    Supplier update(Long companyId, Supplier supplier);
    Supplier partialUpdate(Long companyId, Supplier supplier);
    void deleteSupplierById(Long companyId, Long id);
    List<Supplier> searchSuppliers(Long companyId, String searchTerm);
    Optional<Supplier> findByCompanyIdAndId(Long companyId, Long id);
    
    /**
     * Find paginated suppliers with optional search by name
     * 
     * @param companyId The company ID
     * @param searchTerm Optional search term to filter by name
     * @param pageable Pagination and sorting information
     * @return Page of suppliers
     */
    Page<Supplier> findPaginatedSuppliers(Long companyId, String searchTerm, Pageable pageable);
}
