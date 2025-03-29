package com.rayvision.inventory_management.model.dto;

import jakarta.validation.Valid;
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

    @Valid
    private List<MenuItemLineDTO> menuItemLines;

}
