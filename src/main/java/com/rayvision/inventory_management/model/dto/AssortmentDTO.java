package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.util.Set;

@Data
public class AssortmentDTO {
    private Long id;                  // null for create, or the existing ID for update
    private String name;              // required or optional
    private Long companyId;           // required to link to the correct company

    // The user can pass sets of item IDs or subRecipe IDs
    private Set<Long> itemIds;        // references InventoryItem.id
    private Set<Long> subRecipeIds;   // references SubRecipe.id
    private Set<Long> locationIds;    // references Location.id
    private Set<Long> purchaseOptionIds;   // references PurchaseOption.id

}
