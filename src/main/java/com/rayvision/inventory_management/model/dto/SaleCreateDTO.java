package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SaleCreateDTO {
    private Long locationId;          // which location sold the items
    private LocalDateTime saleDateTime;
    private String posReference;      // check number, etc.
    private List<SaleLineDTO> lines;
}
