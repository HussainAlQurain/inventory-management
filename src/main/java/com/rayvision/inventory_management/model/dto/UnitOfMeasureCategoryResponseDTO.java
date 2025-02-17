package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class UnitOfMeasureCategoryResponseDTO {
    private Long id;
    private String name;        // e.g. "COUNT", "MASS", "VOLUME"
    private String description;
}
