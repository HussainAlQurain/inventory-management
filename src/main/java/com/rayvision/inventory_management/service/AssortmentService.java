package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.Assortment;
import com.rayvision.inventory_management.model.dto.AssortmentDTO;

import java.util.List;
import java.util.Set;

public interface AssortmentService {
    Assortment create(Long companyId, String name);
    Assortment getOne(Long companyId, Long assortmentId);
    List<Assortment> getAll(Long companyId);
    void delete(Long companyId, Long assortmentId);

    // Full Update/Override (if you still want it)
    Assortment update(Long companyId, Long assortmentId, AssortmentDTO dto);

    // Partial Update/Override (if you still want it)
    Assortment partialUpdate(Long companyId, Long assortmentId, AssortmentDTO dto);

    // --- Bulk Add/Remove for Inventory Items ---
    Assortment addInventoryItems(Long companyId, Long assortmentId, Set<Long> itemIds);
    Assortment removeInventoryItems(Long companyId, Long assortmentId, Set<Long> itemIds);

    // --- Bulk Add/Remove for SubRecipes ---
    Assortment addSubRecipes(Long companyId, Long assortmentId, Set<Long> subRecipeIds);
    Assortment removeSubRecipes(Long companyId, Long assortmentId, Set<Long> subRecipeIds);

    // --- Bulk Add/Remove for PurchaseOptions ---
    Assortment addPurchaseOptions(Long companyId, Long assortmentId, Set<Long> purchaseOptionIds);
    Assortment removePurchaseOptions(Long companyId, Long assortmentId, Set<Long> purchaseOptionIds);

    // --- Bulk Add/Remove for Locations (via bridging) ---
    Assortment addLocations(Long companyId, Long assortmentId, Set<Long> locationIds);
    Assortment removeLocations(Long companyId, Long assortmentId, Set<Long> locationIds);

    // Possibly you could do override methods as well, e.g. setInventoryItems(...)

}
