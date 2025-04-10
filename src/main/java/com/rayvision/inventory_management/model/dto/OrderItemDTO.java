package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long inventoryItemId;
    private Double quantity;
    // Price & unitOfMeasureId are removed or ignored here
    // because we don't want them from the user
}
