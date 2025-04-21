package com.rayvision.inventory_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.rayvision.inventory_management.model.InventoryItemLocation;

import java.util.List;
import java.util.Optional;


@Repository
public interface InventoryItemLocationRepository extends JpaRepository<InventoryItemLocation, Long> {
    // find bridging by item + location if you want a unique pair
    Optional<InventoryItemLocation> findByInventoryItemIdAndLocationId(Long itemId, Long locationId);

    // find all bridging for a given item
    List<InventoryItemLocation> findByInventoryItemId(Long itemId);

    // find all bridging for a given location
    List<InventoryItemLocation> findByLocationId(Long locationId);

    /** all rows that belong to locations of a company */
    @Query("""
        SELECT iil
          FROM InventoryItemLocation iil
          WHERE iil.location.company.id = :companyId
    """)
    List<InventoryItemLocation> findAllByCompanyId(Long id);
}
