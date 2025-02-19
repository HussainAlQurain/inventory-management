package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class MenuItemInventoryLineDTO {
    private Long inventoryItemId;     // required
    private Long unitOfMeasureId;     // if needed
    private Double quantity;          // required
    private Double wastagePercent;    // optional
}
