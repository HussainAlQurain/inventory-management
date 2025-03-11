package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class InventoryCountLineDTO {
    private Long id;
    private Long inventoryItemId;     // optional
    private Long subRecipeId;         // optional
    private Long storageAreaId;
    private Double countedQuantity;
    private Long countUomId;
    private Double convertedQuantityInBaseUom;
    private Double lineTotalValue;
}
