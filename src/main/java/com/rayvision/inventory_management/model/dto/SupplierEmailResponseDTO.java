package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class SupplierEmailResponseDTO {
    private Long id;
    private String email;
    private boolean isDefault;
}
