package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class SubRecipeLineDTO {
    private Long id;

    // optional
    private Long inventoryItemId;

    // optional
    private Long childSubRecipeId;

    private Double quantity;
    private Double wastagePercent;
    private Long unitOfMeasureId;
    private Double lineCost; // optional

}
