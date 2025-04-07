package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class StockLevelDTO {
    private Long itemId;          // or null if subRecipeId is used
    private Long subRecipeId;     // or null if itemId is used
    private String name;          // e.g. “Tomatoes” or “Donut Dough”
    private Double onHand;        // The quantity on hand (theoretical)
    private Double cost;          // If you want average cost or total value, you can add more fields
}
