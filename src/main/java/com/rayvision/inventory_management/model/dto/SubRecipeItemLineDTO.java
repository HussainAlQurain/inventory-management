package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class SubRecipeItemLineDTO {
    private Long inventoryItemId;      // required
    private Double quantity;           // required
    private Double wastagePercent;     // optional
    private Long unitOfMeasureId;      // optional

    // If you want them to pass cost or let you compute automatically:
    // private Double cost;
}
