package com.rayvision.inventory_management.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UnitOfMeasureCreateDTO {
    @NotBlank(message = "Unit name is required")
    private String name;
    private String abbreviation;
    // Either provide an existing category ID…
    private Long categoryId;
    // …or provide details for a new UOM category.
    private UnitOfMeasureCategoryCreateDTO category;
    @NotNull
    private Double conversionFactor;

}
