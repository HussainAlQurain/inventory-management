package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class SupplierPhoneResponseDTO {
    private Long id;
    private Long locationId; //newly added
    private String phoneNumber;
    private boolean isDefault;
}
