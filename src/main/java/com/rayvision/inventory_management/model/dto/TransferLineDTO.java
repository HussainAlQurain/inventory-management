package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class TransferLineDTO {
    private Long inventoryItemId;
    private Double quantity;
    private Double costPerUnit; // optional
}
