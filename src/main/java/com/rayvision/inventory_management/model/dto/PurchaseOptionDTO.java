package com.rayvision.inventory_management.model.dto;

import com.rayvision.inventory_management.model.Supplier;
import lombok.Data;

@Data
public class PurchaseOptionDTO {
    private Long supplierId;
    private SupplierDTO supplier;
    private Double price;
    private Double taxRate;
    private Long orderingUomId;
    private UnitOfMeasureCreateDTO orderingUom;
    private Double innerPackQuantity;
    private Double packsPerCase;
    private Double minOrderQuantity;
    private boolean mainPurchaseOption;
    private boolean orderingEnabled;
    private String supplierProductCode;
    private String nickname;
    private String scanBarcode;
}
