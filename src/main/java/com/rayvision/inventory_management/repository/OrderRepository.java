package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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

}
