package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long id;
    private Long inventoryItemId;
    private Double quantity;
    private Double price;
    // Add this field
    private Long purchaseOptionId;
}
