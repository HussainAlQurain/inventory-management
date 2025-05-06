package com.rayvision.inventory_management.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemListDTO {
    private Long id;
    private String name;
    private String sku;
    private String productCode;
    private Double currentPrice;

    // Category info
    private Long categoryId;
    private String categoryName;

    // UOM info
    private Long inventoryUomId;
    private String inventoryUomAbbreviation;

    // Main purchase option info (flattened)
    private Long mainSupplierId;
    private String mainSupplierName;
    private Boolean orderingEnabled;
    private Double taxRate;

    // Stock info
    private Double onHand;
    private Double onHandValue;
}
