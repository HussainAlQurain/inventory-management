package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class UomResponseDTO {
    private Long id;
    private String name;
    private String abbreviation;
    // Possibly the category name or ID as well
    private Long categoryId;
    private String categoryName;
}
