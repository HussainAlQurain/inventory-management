package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.InventoryCountLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryCountLineRepository extends JpaRepository<InventoryCountLine, Long> {
    // Example: find lines by session
    List<InventoryCountLine> findByCountSessionId(Long countSessionId);

    // Example: find lines by session + storage area
    List<InventoryCountLine> findByCountSessionIdAndStorageAreaId(Long sessionId, Long storageAreaId);
}
