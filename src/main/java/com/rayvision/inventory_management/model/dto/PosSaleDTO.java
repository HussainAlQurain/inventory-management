package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PosSaleDTO {
    private Long id;
    private LocalDateTime saleDateTime;
    private String posReference;
    private Long locationId;
    private Double total;

    private List<PosLineDTO> lines;
}
