package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.dto.AvailableItemDTO;
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
    List<InventoryItem> findByCompanyId(Long companyId);
    
    @Query("SELECT i FROM InventoryItem i WHERE i.company.id = :companyId")
    List<InventoryItem> findAllByCompanyId(@Param("companyId") Long companyId);
    
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

    /**
     * Return only the primary‑key values for all items that belong to the company.
     * No entities are loaded, so it is cheap even for thousands of rows.
     */
    @Query("""
        SELECT i.id
        FROM   InventoryItem i
        WHERE  i.company.id = :companyId
    """)
    Set<Long> findIdsByCompany(@Param("companyId") Long companyId);

    // Paginated methods
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
     * Enhanced query that includes UOM information for proper unit conversion
     * 
     * @param locationId The location ID to get inventory data for
     * @param companyId The company ID to filter items by
     * @return Arrays containing all data needed for auto-ordering with UOM information
     */
    @Query("""
        SELECT i.id, i.name, 
               po.id, po.price, po.mainPurchaseOption, 
               s.id, s.name, 
               iil.onHand, iil.minOnHand, iil.parLevel, 
               po.orderingUom.abbreviation, po.orderingUom.conversionFactor, 
               i.inventoryUom.abbreviation
        FROM InventoryItem i 
        JOIN i.purchaseOptions po 
        JOIN po.supplier s 
        JOIN InventoryItemLocation iil ON iil.inventoryItem.id = i.id 
        WHERE i.company.id = :companyId 
        AND iil.location.id = :locationId 
        AND po.orderingEnabled = true
    """)
    List<Object[]> findCompleteAutoOrderDataByLocationAndCompany(
        @Param("locationId") Long locationId,
        @Param("companyId") Long companyId
    );

    /**
     * Similar to findCompleteAutoOrderDataByLocationAndCompany but filters by item IDs
     */
    @Query("""
        SELECT i.id, i.name, 
               po.id, po.price, po.mainPurchaseOption, 
               s.id, s.name, 
               iil.onHand, iil.minOnHand, iil.parLevel, 
               po.orderingUom.abbreviation, po.orderingUom.conversionFactor, 
               i.inventoryUom.abbreviation
        FROM InventoryItem i 
        JOIN i.purchaseOptions po 
        JOIN po.supplier s 
        JOIN InventoryItemLocation iil ON iil.inventoryItem.id = i.id 
        WHERE i.id IN :itemIds 
        AND iil.location.id = :locationId 
        AND po.orderingEnabled = true
    """)
    List<Object[]> findCompleteAutoOrderDataByItemIdsAndLocation(
        @Param("itemIds") Collection<Long> itemIds,
        @Param("locationId") Long locationId
    );
    
    /**
     * Optimized query for retrieving inventory items available for order from a specific supplier
     * with pagination, search, and optional assortment filtering directly in the database.
     * Fixed version that removes the IS EMPTY check for parameters.
     *
     * @param supplierId Supplier ID to filter items by
     * @param assortmentIds Optional list of assortment IDs to filter items by
     * @param search Optional search term to filter items by name/sku/productCode
     * @param pageable Pagination and sorting parameters
     * @return Page of inventory items available for ordering
     */
    @Query(value = """
        SELECT DISTINCT i
        FROM InventoryItem i
        JOIN i.purchaseOptions po
        JOIN po.supplier s
        WHERE s.id = :supplierId
        AND po.orderingEnabled = true
        AND (:assortmentIds IS NULL OR i.id IN (
            SELECT ai.id FROM Assortment a
            JOIN a.inventoryItems ai
            WHERE a.id IN :assortmentIds
        ))
        AND (COALESCE(:search, '') = ''
            OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(i.sku) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(i.productCode) LIKE LOWER(CONCAT('%', :search, '%')))
        """,
        countQuery = """
        SELECT COUNT(DISTINCT i)
        FROM InventoryItem i
        JOIN i.purchaseOptions po
        JOIN po.supplier s
        WHERE s.id = :supplierId
        AND po.orderingEnabled = true
        AND (:assortmentIds IS NULL OR i.id IN (
            SELECT ai.id FROM Assortment a
            JOIN a.inventoryItems ai
            WHERE a.id IN :assortmentIds
        ))
        AND (COALESCE(:search, '') = ''
            OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(i.sku) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(i.productCode) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<InventoryItem> findAvailableItems(
        @Param("supplierId") Long supplierId,
        @Param("assortmentIds") List<Long> assortmentIds,
        @Param("search") String search,
        Pageable pageable
    );
    
    /**
     * Optimized DTO projection query for retrieving only necessary inventory item data
     * for available items from a specific supplier with pagination, search, and optional 
     * assortment filtering directly in the database.
     */
    @Query(value = """
        SELECT new com.rayvision.inventory_management.model.dto.AvailableItemDTO(
            i.id,
            i.name,
            i.sku,
            i.productCode,
            po.price,
            COALESCE(i.inventoryUom.abbreviation, ''),
            COALESCE(po.orderingUom.abbreviation, ''),
            po.mainPurchaseOption
        )
        FROM InventoryItem i
        JOIN i.purchaseOptions po
        JOIN po.supplier s
        LEFT JOIN i.inventoryUom
        LEFT JOIN po.orderingUom
        WHERE s.id = :supplierId
        AND po.orderingEnabled = true
        AND (:assortmentIds IS NULL OR i.id IN (
            SELECT ai.id FROM Assortment a
            JOIN a.inventoryItems ai
            WHERE a.id IN :assortmentIds
        ))
        AND (COALESCE(:search, '') = ''
            OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(i.sku) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(i.productCode) LIKE LOWER(CONCAT('%', :search, '%')))
        """,
        countQuery = """
        SELECT COUNT(DISTINCT i)
        FROM InventoryItem i
        JOIN i.purchaseOptions po
        JOIN po.supplier s
        WHERE s.id = :supplierId
        AND po.orderingEnabled = true
        AND (:assortmentIds IS NULL OR i.id IN (
            SELECT ai.id FROM Assortment a
            JOIN a.inventoryItems ai
            WHERE a.id IN :assortmentIds
        ))
        AND (COALESCE(:search, '') = ''
            OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(i.sku) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(i.productCode) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<AvailableItemDTO> findAvailableItemsDto(
        @Param("supplierId") Long supplierId,
        @Param("assortmentIds") List<Long> assortmentIds,
        @Param("search") String search,
        Pageable pageable
    );
}
