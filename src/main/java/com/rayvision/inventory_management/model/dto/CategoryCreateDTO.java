package com.rayvision.inventory_management.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryCreateDTO {
    @NotBlank(message = "Category name is required")
    private String name;

    private String description;
}
