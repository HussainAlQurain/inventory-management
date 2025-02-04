package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> company(Company company);
    List<InventoryItem> findByCompanyId(Long companyId);
    Optional<InventoryItem> findByCompanyIdAndId(Long companyId, Long inventoryId);
}
