package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class MenuItemLineResponseDTO {
    private Long id;
    private Long inventoryItemId;
    private Long subRecipeId;
    private Long childMenuItem;
    private Double quantity;
    private Double wastagePercent;
    private Long unitOfMeasureId;
    private Double lineCost;
}
