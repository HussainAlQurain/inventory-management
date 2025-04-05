package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class CountUomPreferenceDTO {
    private Long id;
    private Long inventoryItemId;  // null if this preference is for a SubRecipe
    private Long subRecipeId;      // null if this preference is for an Item
    private Long countUomId;
    private Boolean defaultUom;
}
