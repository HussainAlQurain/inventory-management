package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class TransferLineDTO {
    private Long id;                    // null when creating
    private Long inventoryItemId;       // or subRecipeId (add if needed)
    private Long subRecipeId;

    private String itemName;            // filled on GET only
    private Double quantity;

    private Long unitOfMeasureId;
    private String uomName;             // filled on GET only

    private Double costPerUnit;
    private Double totalCost;
}
