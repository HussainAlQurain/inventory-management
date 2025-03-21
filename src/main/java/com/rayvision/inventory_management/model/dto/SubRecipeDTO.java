package com.rayvision.inventory_management.model.dto;

import com.rayvision.inventory_management.enums.SubRecipeType;
import lombok.Data;

@Data
public class SubRecipeDTO {
    private Long id;
    private String name;
    private SubRecipeType type;
    private Long categoryId;
    private Double cost;
    private Long uomId;
    private Double yieldQty;
    private String photoUrl;
    private Integer prepTimeMinutes;
    private Integer cookTimeMinutes;
    private String instructions;
}
