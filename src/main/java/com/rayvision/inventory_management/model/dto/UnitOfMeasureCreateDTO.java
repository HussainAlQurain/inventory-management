package com.rayvision.inventory_management.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UnitOfMeasureCreateDTO {
    @NotBlank(message = "Unit name is required")
    private String name;
    private String abbreviation;
    @NotBlank
    private String category;
    @NotNull
    private Double conversionFactor;
}
