package com.rayvision.inventory_management.model.dto;

import com.rayvision.inventory_management.enums.SubRecipeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubRecipeListDTO {
    private Long id;
    private String name;
    private SubRecipeType type;
    private Long categoryId;
    private String categoryName;
    private Long uomId;
    private String uomName;
    private String uomAbbreviation;
    private Double yieldQty;
    private Double cost;
    // Essential fields only - no lines or other heavy data
}
