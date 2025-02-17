package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class InventoryItemResponseDTO {
    private Long id;
    private String name;
    private String sku;
    private String productCode;
    private String description;
    private Double currentPrice;
    private Double calories;

    // The category
    private CategoryResponseDTO category;

    // The base inventory UOM
    private UnitOfMeasureResponseDTO inventoryUom;

    // The purchase options
    private List<PurchaseOptionResponseDTO> purchaseOptions;
}
