package com.rayvision.inventory_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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

}
