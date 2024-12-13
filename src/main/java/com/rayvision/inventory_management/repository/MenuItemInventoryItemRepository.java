package com.rayvision.inventory_management.repository;

import com.rayvision.inventory_management.model.MenuItemInventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuItemInventoryItemRepository extends JpaRepository<MenuItemInventoryItem, Long> {
}
