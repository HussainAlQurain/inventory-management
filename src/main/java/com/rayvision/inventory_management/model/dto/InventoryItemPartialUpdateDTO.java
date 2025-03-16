package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class InventoryItemPartialUpdateDTO {
    private String name;
    private String sku;
    private String productCode;
    private String description;
    private Double currentPrice;
    private Double calories;
    private Long categoryId;
    // ...or provide details to create a new one:
    private CategoryCreateDTO category;
}
