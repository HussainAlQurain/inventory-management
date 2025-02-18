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

    // If you want to show some supplier info:
    private Long supplierId;
    private String supplierName;

    private UnitOfMeasureResponseDTO orderingUom;
    private SupplierResponseDTO supplier;

    // If you want to show ordering UOM info:
    private Long orderingUomId;
    private String orderingUomName;
    private String orderingUomAbbreviation;

}
