package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class SupplierPhonePartialUpdateDTO {
    private Long id;
    private String phoneNumber;
    private boolean isDefault;
    /**
     * Optional location id for which this phone number is the default.
     * If null, then this phone number is a global default.
     */
    private Long locationId;
}
