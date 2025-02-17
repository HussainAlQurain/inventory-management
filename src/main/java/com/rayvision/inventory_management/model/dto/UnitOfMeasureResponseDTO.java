package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class UnitOfMeasureResponseDTO {
    private Long id;
    private String name;          // e.g. "Box"
    private String abbreviation;   // e.g. "BOX"
    private Double conversionFactor;

    // If you want to show the category's name/ID too:
    private UnitOfMeasureCategoryResponseDTO category;

}
