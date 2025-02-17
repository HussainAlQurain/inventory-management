package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class PurchaseOptionResponseDTO {
    private Long id;
    private Double price;
    private Double taxRate;
    private Double innerPackQuantity;
    private Double packsPerCase;
    private Double minOrderQuantity;
    private boolean mainPurchaseOption;
    private boolean orderingEnabled;
    private String supplierProductCode;
    private String nickname;
    private String scanBarcode;

    // The ordering UOM
    private UnitOfMeasureResponseDTO orderingUom;

    // The supplier
    private SupplierResponseDTO supplier;
}
