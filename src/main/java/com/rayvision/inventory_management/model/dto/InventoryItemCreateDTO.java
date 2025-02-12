package com.rayvision.inventory_management.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class InventoryItemCreateDTO {
    @NotBlank(message = "Name is required")
    private String name;
    private String sku;
    private String productCode;
    private String description;
    private Double currentPrice = 0.0;
    private Double calories = 0.0;
    private Long companyId;
    // You can use either the ID for an existing category...
    private Long categoryId;
    // ...or provide details to create a new one:
    private CategoryCreateDTO category;
    // Similarly for unit of measure:
    private Long inventoryUomId;
    private UnitOfMeasureCreateDTO inventoryUom;
    // List of purchase options related to the inventory item.
    private List<PurchaseOptionDTO> purchaseOptions;
}
