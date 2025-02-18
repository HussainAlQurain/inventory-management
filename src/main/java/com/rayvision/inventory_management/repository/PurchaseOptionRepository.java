package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.PurchaseOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOptionRepository extends JpaRepository<PurchaseOption, Long> {
    List<PurchaseOption> findByInventoryItemId(Long inventoryItemId);
}
