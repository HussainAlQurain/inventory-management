package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class SaleLineCreateDTO {
    private String posCode;          // if we want to store
    private String menuItemName;
    private Double quantity;
    private Double unitPrice;
}
