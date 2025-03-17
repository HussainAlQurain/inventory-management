package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class PurchaseOptionPartialUpdateDTO {
    private Long supplierId;
    private Double taxRate;
    private Long orderingUomId;
    private Double innerPackQuantity;
    private Double packsPerCase;
    private Double minOrderQuantity;
    private Boolean mainPurchaseOption;
    private Boolean orderingEnabled;
    private String supplierProductCode;
    private String nickname;
    private String scanBarcode;
}
