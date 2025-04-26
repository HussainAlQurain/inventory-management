package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.enums.OrderStatus;
import com.rayvision.inventory_management.model.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Orders, Long>, JpaSpecificationExecutor<Orders> {
    @Query("SELECT o FROM Orders o "
            + "WHERE o.company.id = :companyId "
            + "AND o.creationDate >= :start "
            + "AND o.creationDate <= :end "
            + "ORDER BY o.creationDate DESC")
    List<Orders> findByCompanyIdAndDateRange(@Param("companyId") Long companyId,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    @Query("SELECT o FROM Orders o "
            + "WHERE o.sentToSupplier.id = :supplierId "
            + "  AND o.buyerLocation.id = :locationId "
            + "  AND o.status = 'DRAFT'"
            + "  AND o.createdByUser.id = :systemUserId")
    Optional<Orders> findSystemDraft(@Param("supplierId") Long supplierId,
                                     @Param("locationId") Long locationId,
                                     @Param("systemUserId") Long systemUserId);

    @Query("SELECT o FROM Orders o "
            + "WHERE o.sentToSupplier.id = :supplierId "
            + "  AND o.buyerLocation.id = :locationId "
            + "  AND o.status = 'DRAFT'"
            + "  AND o.createdByUser.id = :systemUserId")
    List<Orders> findAllSystemDrafts(@Param("supplierId") Long supplierId,
                                     @Param("locationId") Long locationId,
                                     @Param("systemUserId") Long systemUserId);
    
    @Query("SELECT o FROM Orders o "
           + "WHERE o.status = 'DRAFT' "
           + "AND o.sentToSupplier.id = :supplierId "
           + "AND o.buyerLocation.id = :locationId")
    List<Orders> findDraftOrdersBySupplierAndLocation(
           @Param("supplierId") Long supplierId,
           @Param("locationId") Long locationId);
                                                    
    /**
     * Find all orders with a specific status for a given buyer location
     */
    List<Orders> findByBuyerLocationIdAndStatus(Long buyerLocationId, OrderStatus status);
    
    // New paginated methods
    @Query("SELECT o FROM Orders o "
            + "WHERE o.company.id = :companyId "
            + "AND o.creationDate >= :start "
            + "AND o.creationDate <= :end "
            + "ORDER BY o.creationDate DESC")
    Page<Orders> findByCompanyIdAndDateRange(
            @Param("companyId") Long companyId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);
    
    /**
     * Find all orders with a specific status for a given buyer location with pagination
     */
    Page<Orders> findByBuyerLocationIdAndStatus(Long buyerLocationId, OrderStatus status, Pageable pageable);
    
    /**
     * Find all orders for a company with pagination
     */
    Page<Orders> findByCompanyId(Long companyId, Pageable pageable);
    
    /**
     * Find all orders for a company with specific status
     */
    Page<Orders> findByCompanyIdAndStatus(Long companyId, OrderStatus status, Pageable pageable);
    
    /**
     * Find all orders by status with pagination
     */
    Page<Orders> findByStatus(OrderStatus status, Pageable pageable);
    
    /**
     * Advanced search for orders with filtering options
     */
    @Query("SELECT o FROM Orders o "
            + "WHERE o.company.id = :companyId "
            + "AND (:supplierId IS NULL OR o.sentToSupplier.id = :supplierId) "
            + "AND (:locationId IS NULL OR o.buyerLocation.id = :locationId) "
            + "AND (:status IS NULL OR o.status = :status) "
            + "AND o.creationDate >= :start "
            + "AND o.creationDate <= :end "
            + "ORDER BY o.creationDate DESC")
    Page<Orders> searchOrders(
            @Param("companyId") Long companyId,
            @Param("supplierId") Long supplierId,
            @Param("locationId") Long locationId,
            @Param("status") OrderStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);
            
    /**
     * Get in-transit quantities for items at a specific location.
     * Returns arrays of [itemId, inTransitQty]
     * 
     * This query calculates quantities for items that have been ordered but not yet
     * fully delivered, based on orders that are not in DRAFT or CANCELED status.
     */
    @Query("SELECT oi.inventoryItem.id, SUM(oi.quantity) " +
           "FROM Orders o " +
           "JOIN o.orderItems oi " +
           "WHERE o.buyerLocation.id = :locationId " +
           "AND o.status = 'SENT' " +
           "GROUP BY oi.inventoryItem.id")
    List<Object[]> getInTransitQuantitiesByLocation(@Param("locationId") Long locationId);
    
    /**
     * Get quantities for items in DRAFT and in-progress orders at a specific location.
     * Returns arrays of [itemId, quantity]
     */
    @Query("""
    SELECT oi.inventoryItem.id, SUM(oi.quantity)
    FROM   Orders o
    JOIN   o.orderItems oi
    WHERE  o.buyerLocation.id = :locationId
      AND  o.status IN ('DRAFT', 'CREATED', 'SUBMITTED_FOR_APPROVAL',
                      'APPROVED')
    GROUP  BY oi.inventoryItem.id
    """)
    List<Object[]> getDraftAndPendingQuantitiesByLocation(@Param("locationId") Long locationId);
    
    /**
     * Find all draft orders created by system-user for a specific location
     */
    @Query("""
    SELECT o
    FROM   Orders o
    WHERE  o.buyerLocation.id  = :locationId
      AND  o.status            = 'DRAFT'
      AND  o.createdByUser.id  = :systemUserId
    """)
    List<Orders> findAllSystemDraftsByLocation(@Param("locationId") Long locationId,
                                              @Param("systemUserId") Long systemUserId);
}
