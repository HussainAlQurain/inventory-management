package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class MenuItemCreateDTO {
    private String name;
    private String posCode;
    private Long categoryId;
    private Double retailPriceExclTax;
    private Double maxAllowedFoodCostPct;

    // Possibly other fields:
    private String modifierGroups;
    private Double cost; // or you can compute
    private Double foodCostPercentage;

    /**
     * If the user wants to attach bridging lines to raw InventoryItems:
     */
    private List<MenuItemInventoryLineDTO> inventoryLines;

    /**
     * If the user wants bridging lines to subRecipes:
     */
    private List<MenuItemSubRecipeLineDTO> subRecipeLines;

}
