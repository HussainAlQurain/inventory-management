package com.rayvision.inventory_management.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optimized lightweight DTO for available inventory items query
 * Used in the DTO projection for the `available-items` endpoint
 * to minimize database round-trips and improve performance
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvailableItemDTO {
    private Long id;
    private String name;
    private String sku;
    private String productCode;
    private Double price;
    private String inventoryUom;
    private String orderingUom;
    private boolean mainPurchaseOption;
}