package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long inventoryItemId;
    private Double quantity;
    private Double price;
    private Long unitOfMeasureId; // optional
}
