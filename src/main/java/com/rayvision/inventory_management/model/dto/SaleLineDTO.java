package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class SaleLineDTO {
    // If you know the menuItem ID, pass it. If not, pass posCode.
    private Long menuItemId;    // optional
    private String posCode;     // required if you don’t have menuItemId
    private String menuItemName;// optional fallback name

    private Double quantity;    // how many sold
    private Double unitPrice;   // price per unit at sale time
}
