package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class MenuItemSubRecipeLineDTO {
    private Long subRecipeId;         // required
    private Long unitOfMeasureId;     // if needed
    private Double quantity;          // required
    private Double wastagePercent;    // optional
}
