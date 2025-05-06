package com.rayvision.inventory_management.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UomFilterOptionDTO {
    private Long id;
    private String name;
    private String abbreviation;
}
