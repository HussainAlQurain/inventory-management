package com.rayvision.inventory_management.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UnitOfMeasureCategoryCreateDTO {
    @NotBlank(message = "UOM Category name is required")
    private String name;
    private String description;
}
