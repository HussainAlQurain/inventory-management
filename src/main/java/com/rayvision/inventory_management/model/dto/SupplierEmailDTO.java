package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class SupplierEmailDTO {
    private Long id;
    private String email;
    private boolean isDefault;
    /**
     * Optional location id for which this email is the default.
     * If null, then this email is a global default.
     */
    private Long locationId;
}
