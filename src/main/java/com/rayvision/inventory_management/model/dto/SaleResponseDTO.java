package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SaleResponseDTO {
    private Long saleId;
    private Long locationId;
    private LocalDateTime saleDateTime;
    private String posReference;
    private Double totalRevenue;
    private Double totalCost;
    private Double totalProfit;
    private List<SaleLineResponseDTO> lines;
}
