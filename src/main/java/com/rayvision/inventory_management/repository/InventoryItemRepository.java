package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.model.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    @Query("SELECT i FROM InventoryItem i LEFT JOIN FETCH i.purchaseOptions WHERE i.company.id = :companyId")
    List<InventoryItem> findByCompanyIdWithPurchaseOptions(@Param("companyId") Long companyId);

    @Query("SELECT i FROM InventoryItem i LEFT JOIN FETCH i.purchaseOptions WHERE i.id = :id")
    Optional<InventoryItem> findByIdWithPurchaseOptions(@Param("id") Long id);

    /**
     * Efficiently retrieves only the necessary data for auto-ordering in a single query
     * Returns results as Object[] arrays containing:
     * [0] - item.id (Long)
     * [1] - item.name (String)
     * [2] - po.id (Long)
     * [3] - po.price (Double)
     * [4] - supplier.id (Long)
     * [5] - supplier.name (String)
     */
    @Query("SELECT i.id, i.name, po.id, po.price, s.id, s.name " +
           "FROM InventoryItem i " +
           "JOIN i.purchaseOptions po " +
           "JOIN po.supplier s " +
           "WHERE i.company.id = :companyId " +
           "AND po.orderingEnabled = true")
    List<Object[]> findAutoOrderDataByCompanyId(@Param("companyId") Long companyId);

    /**
     * Similar to findAutoOrderDataByCompanyId but filters by item IDs
     */
    @Query("SELECT i.id, i.name, po.id, po.price, s.id, s.name " +
           "FROM InventoryItem i " +
           "JOIN i.purchaseOptions po " +
           "JOIN po.supplier s " +
           "WHERE i.id IN :itemIds " +
           "AND po.orderingEnabled = true")
    List<Object[]> findAutoOrderDataByItemIds(@Param("itemIds") Collection<Long> itemIds);

    /**
     * Comprehensive query that retrieves all data needed for auto-ordering in a single operation.
     * Returns all necessary data to populate the enhanced ItemOrderInfoDTO.
     * 
     * @param locationId The location ID to get inventory data for
     * @param companyId The company ID to filter items by
     * @return Array of Object[] containing all data needed for auto-ordering
     */
    @Query("SELECT i.id, i.name, " +
           "po.id, po.price, po.mainPurchaseOption, " +
           "s.id, s.name, " +
           "iil.onHand, iil.minOnHand, iil.parLevel " +
           "FROM InventoryItem i " +
           "JOIN i.purchaseOptions po " +
           "JOIN po.supplier s " +
           "JOIN InventoryItemLocation iil ON iil.inventoryItem.id = i.id " +
           "WHERE i.company.id = :companyId " +
           "AND iil.location.id = :locationId " +
           "AND po.orderingEnabled = true " +
           "AND (iil.parLevel > 0 OR iil.minOnHand > 0)")
    List<Object[]> findCompleteAutoOrderDataByLocationAndCompany(
        @Param("locationId") Long locationId,
        @Param("companyId") Long companyId
    );

    /**
     * Similar to findCompleteAutoOrderDataByLocationAndCompany but filters by item IDs
     */
    @Query("SELECT i.id, i.name, " +
           "po.id, po.price, po.mainPurchaseOption, " +
           "s.id, s.name, " +
           "iil.onHand, iil.minOnHand, iil.parLevel " +
           "FROM InventoryItem i " +
           "JOIN i.purchaseOptions po " +
           "JOIN po.supplier s " +
           "JOIN InventoryItemLocation iil ON iil.inventoryItem.id = i.id " +
           "WHERE i.id IN :itemIds " +
           "AND iil.location.id = :locationId " +
           "AND po.orderingEnabled = true " +
           "AND (iil.parLevel > 0 OR iil.minOnHand > 0)")
    List<Object[]> findCompleteAutoOrderDataByItemIdsAndLocation(
        @Param("itemIds") Collection<Long> itemIds,
        @Param("locationId") Long locationId
    );
}
