package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class SaleLineResponseDTO {
    private Long saleLineId;
    private Long menuItemId;
    private String menuItemName;
    private String posCode;

    private Double quantity;
    private Double unitPriceAtSale;
    private Double extendedPrice;
    private Double costAtSaleTime;
    private Double profitAtSaleTime;
}
