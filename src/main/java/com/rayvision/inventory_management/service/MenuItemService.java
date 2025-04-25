package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.MenuItem;
import com.rayvision.inventory_management.model.dto.MenuItemCreateDTO;
import com.rayvision.inventory_management.model.dto.MenuItemLineDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    /**
     * Search menu items with pagination support
     * @param companyId The company ID
     * @param searchTerm Search term to filter results
     * @param pageable Pagination parameters
     * @return Page of menu items
     */
    Page<MenuItem> searchMenuItems(Long companyId, String searchTerm, Pageable pageable);

    /**
     * Search menu items without pagination
     * @param companyId The company ID
     * @param searchTerm Search term to filter results
     * @return List of menu items
     */
    List<MenuItem> searchMenuItems(Long companyId, String searchTerm);

    MenuItem addLineToMenuItem(Long companyId, Long menuItemId, MenuItemLineDTO lineDTO);
    MenuItem removeLineFromMenuItem(Long companyId, Long menuItemId, Long lineId);
    MenuItem updateLine(Long companyId, Long menuItemId, MenuItemLineDTO dto);

}
