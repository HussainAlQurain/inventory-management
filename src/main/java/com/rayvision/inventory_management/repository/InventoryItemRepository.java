package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.model.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> company(Company company);
    List<InventoryItem> findByCompanyId(Long companyId);
    Optional<InventoryItem> findByCompanyIdAndId(Long companyId, Long inventoryId);

    @Query("""
    SELECT i
    FROM InventoryItem i
    WHERE i.company.id = :companyId
      AND (:searchTerm = '' 
           OR LOWER(i.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """)
    List<InventoryItem> searchInventoryItems(
            @Param("companyId") Long companyId,
            @Param("searchTerm") String searchTerm
    );

    /**     /**
     * Return only the primary‑key values for all items that belong to the company.
     * No entities are loaded, so it is cheap even for thousands of rows.
     */
    @Query("""
        SELECT i.id
        FROM   InventoryItem i
        WHERE  i.company.id = :companyId
    """)
    Set<Long> findIdsByCompany(@Param("companyId") Long companyId);

    // New paginated methods
    Page<InventoryItem> findByCompanyId(Long companyId, Pageable pageable);
    
    @Query("""
    SELECT i
    FROM InventoryItem i
    WHERE i.company.id = :companyId
      AND (:searchTerm = '' 
           OR LOWER(i.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """)
    Page<InventoryItem> searchInventoryItems(
            @Param("companyId") Long companyId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );
    
    @Query("""
    SELECT i
    FROM InventoryItem i
    WHERE i.company.id = :companyId
      AND (:categoryId IS NULL OR i.category.id = :categoryId)
      AND (:searchTerm = '' 
           OR LOWER(i.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """)
    Page<InventoryItem> findByCompanyIdAndCategoryWithSearch(
            @Param("companyId") Long companyId,
            @Param("categoryId") Long categoryId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );
}
