package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class TransferLineDTO {
    private Long inventoryItemId;
    private Double quantity;
    private Double costPerUnit;  // optional
    private Double totalCost;    // optional
    private Long unitOfMeasureId; // new, so we know the user’s chosen UOM
}
