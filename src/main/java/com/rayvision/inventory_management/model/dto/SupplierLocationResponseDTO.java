package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class SupplierLocationResponseDTO {
    private Long id;
    private Long locationId; // or a nested LocationResponseDTO if you want details
    // Possibly also the supplierId
}
