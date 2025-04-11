package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("""
        SELECT s 
        FROM Sale s 
        WHERE s.location.id = :locationId
          AND s.saleDateTime >= :start 
          AND s.saleDateTime <= :end
        """)
    List<Sale> findAllByLocationIdAndSaleDateTimeBetween(
            @Param("locationId") Long locationId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    Boolean existsByPosReferenceAndLocationId(String posReference, Long locationId);

}
