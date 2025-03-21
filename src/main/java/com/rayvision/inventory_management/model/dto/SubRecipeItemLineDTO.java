package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class SubRecipeItemLineDTO {
    private Long id;
    private Long inventoryItemId;      // required
    private Double quantity;           // required
    private Double wastagePercent;     // optional
    private Long unitOfMeasureId;      // optional
    private Double cost;
}
