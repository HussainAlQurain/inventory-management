package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class PosSalesPagedResponse {
    private long totalItems;
    private int totalPages;
    private int currentPage;

    private List<PosSaleDTO> sales;
}
