package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class OrderSummaryDTO {
    private Long id;
    private String orderNumber;
    private LocalDate sentDate;
    private LocalDate deliveryDate;
    private String buyerLocationName;
    private String supplierName;
    private Double total;
    private String status;
    private String comments;
}
