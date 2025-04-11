package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.enums.OrderStatus;
import com.rayvision.inventory_management.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository <Orders, Long> {
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
            + "  AND o.createdByUser.id = 999999999")
    Optional<Orders> findDraftBySupplierAndLocation(@Param("supplierId") Long supplierId,
                                                    @Param("locationId") Long locationId);

    @Query("SELECT o FROM Orders o "
            + "WHERE o.sentToSupplier.id = :supplierId "
            + "  AND o.buyerLocation.id = :locationId "
            + "  AND o.status = 'DRAFT'"
            + "  AND o.createdByUser.id = 999999999")
    List<Orders> findAllDraftsBySupplierAndLocation(@Param("supplierId") Long supplierId,
                                                    @Param("locationId") Long locationId);
                                                    
    /**
     * Find all orders with a specific status for a given buyer location
     */
    List<Orders> findByBuyerLocationIdAndStatus(Long buyerLocationId, OrderStatus status);
}
