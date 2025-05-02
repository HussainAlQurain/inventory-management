package com.rayvision.inventory_management.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for returning item on-hand quantity totals and value totals
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemOnHandTotalsDTO {
    private Long itemId;
    private Double totalQuantity;
    private Double totalValue;
}