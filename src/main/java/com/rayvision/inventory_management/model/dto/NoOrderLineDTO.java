package com.rayvision.inventory_management.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NoOrderLineDTO {
    private Long itemId;
    private Long uomId;       // optional, fallback to item’s inventoryUom
    private Double quantity;
    private Double price;
}
