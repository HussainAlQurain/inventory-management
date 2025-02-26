package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class InventoryCountLineDTO {
    private Long id;
    private Long inventoryItemId;
    private Long storageAreaId;
    private Double countedQuantity;
    private Long countUomId;

    private Double convertedQuantityInBaseUom; // optional
    private Double lineTotalValue;             // optional
}
