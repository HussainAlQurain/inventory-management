package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.MenuItem;
import com.rayvision.inventory_management.model.dto.MenuItemCreateDTO;

import java.util.List;
import java.util.Optional;

public interface MenuItemService {
    List<MenuItem> getAllMenuItems(Long companyId);

    Optional<MenuItem> getMenuItemById(Long companyId, Long menuItemId);

    /**
     * Creates a new menu item (and bridging lines, if provided).
     */
    MenuItem createMenuItem(Long companyId, MenuItemCreateDTO dto);

    /**
     * Update (full) the menu item, possibly also bridging lines.
     */
    MenuItem updateMenuItem(Long companyId, Long menuItemId, MenuItemCreateDTO dto);

    /**
     * Partial update (if you only want to update some fields, or bridging lines separately).
     */
    MenuItem partialUpdateMenuItem(Long companyId, Long menuItemId, MenuItemCreateDTO dto);

    /**
     * Delete the menu item by ID.
     */
    void deleteMenuItemById(Long companyId, Long menuItemId);

    /**
     * Optionally, you can have a method that re-computes cost from bridging lines
     * and updates the menuItem cost + foodCostPercentage.
     */
    MenuItem recalcMenuItemCost(MenuItem menuItem);

}
