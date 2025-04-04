package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.InventoryCountSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryCountSessionRepository extends JpaRepository<InventoryCountSession, Long> {

    List<InventoryCountSession> findByLocationId(Long locationId);
    Optional<InventoryCountSession> findByLocationIdAndId(Long locationId, Long sessionId);

    @Query("SELECT s FROM InventoryCountSession s WHERE s.location.id = :locationId AND s.locked = false")
    List<InventoryCountSession> findOpenSessionsByLocationId(@Param("locationId") Long locationId);


    @Query("""
        SELECT DISTINCT ics
          FROM InventoryCountSession ics
          JOIN ics.location loc
         WHERE loc.company.id = :companyId
           AND ics.countDate BETWEEN :startDate AND :endDate
         ORDER BY ics.countDate DESC
    """)
    List<InventoryCountSession> findByCompanyAndDateRange(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
