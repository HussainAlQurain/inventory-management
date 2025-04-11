package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SaleSummaryDTO {
    private Long saleId;
    private LocalDateTime saleDateTime;
    private Long locationId;
    private String posReference;
    private Double totalRevenue;
    private Double totalCost;
    private Double totalProfit;
}
