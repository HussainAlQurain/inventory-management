package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class PurchaseOptionCreateDTO {
    private Long supplierId;
    private Double price;
    private Double taxRate;
    private Long orderingUomId;
    private Double innerPackQuantity;
    private Double packsPerCase;
    private Double minOrderQuantity;
    private boolean mainPurchaseOption;
    private boolean orderingEnabled;
    private String supplierProductCode;
    private String nickname;
    private String scanBarcode;
}
