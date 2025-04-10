package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class OrderItemResponseDTO {
    private Long orderItemId;
    private Long inventoryItemId;
    private String inventoryItemName;
    private Double quantity;
    private Double price;
    private Double total;

    // Possibly the UOM name or ID
    private String uomName;
}
