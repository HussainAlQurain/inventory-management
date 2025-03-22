package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class MenuItemResponseDTO {
    private Long id;
    private String name;
    private String posCode;
    private Double cost;
    private Double retailPriceExclTax;
    private Double foodCostPercentage;
    private Double maxAllowedFoodCostPct;
    private String modifierGroups;
    private CategoryResponseDTO category;
    private List<MenuItemInventoryLineDTO> inventoryLines;
    private List<MenuItemSubRecipeLineDTO> subRecipeLines;
}
