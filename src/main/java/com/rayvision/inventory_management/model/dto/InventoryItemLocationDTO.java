package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class InventoryItemLocationDTO {
    private Long id;          // for updates
    private Long inventoryItemId;
    private Long locationId;
    private Double minOnHand;
    private Double parLevel;
    private Double onHand;        // optional
    private Double lastCount;     // optional
    private LocalDate lastCountDate;
}
