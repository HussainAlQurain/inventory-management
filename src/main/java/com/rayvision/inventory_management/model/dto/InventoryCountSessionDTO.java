package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class InventoryCountSessionDTO {
    private Long id;
    private LocalDate countDate;
    private String dayPart;
    private String description;
    private boolean locked;
    private LocalDate lockedDate;

    // We do not create location on the fly; user passes locationId
    private Long locationId;

    // Each session can have multiple lines
    private List<InventoryCountLineDTO> lines;
}
