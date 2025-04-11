package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class PosLineDTO {
    private Long id;
    private String posCode;
    private String menuItemName;
    private Double quantity;
    private Double unitPrice;
    private Double extended;
}
