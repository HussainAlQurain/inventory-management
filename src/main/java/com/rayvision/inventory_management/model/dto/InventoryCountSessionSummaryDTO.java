package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class InventoryCountSessionSummaryDTO {
    private Long id;
    private LocalDate countDate;
    private String dayPart;
    private String locationName;
    private Double valueOfCount;
    private String description;
}
