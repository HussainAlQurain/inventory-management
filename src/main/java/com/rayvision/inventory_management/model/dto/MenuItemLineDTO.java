package com.rayvision.inventory_management.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MenuItemLineDTO {
    private Long id;

    // Only one of these should be present
    private Long inventoryItemId;
    private Long subRecipeId;
    private Long childMenuItemId;

    @NotNull
    private Double quantity;

    private Double wastagePercent;

    @NotNull
    private Long unitOfMeasureId;

    private Double lineCost;

}
