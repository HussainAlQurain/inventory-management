package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class PurchaseOptionSummaryDTO {
    private Long purchaseOptionId;        // The PurchaseOption's ID
    private String inventoryItemName;     // e.g. "Tomatoes"
    private String purchaseOptionNickname;// e.g. "Tomatoes - Fresh"
    private String orderingUomName;       // e.g. "CASE"
    private Double price;                // e.g. 120.00
    private String categoryName;         // from the InventoryItem's category (if any)
    private String supplierName;
}
