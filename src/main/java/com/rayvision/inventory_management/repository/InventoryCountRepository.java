package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.InventoryCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryCountRepository extends JpaRepository<InventoryCount, Long> {
}
