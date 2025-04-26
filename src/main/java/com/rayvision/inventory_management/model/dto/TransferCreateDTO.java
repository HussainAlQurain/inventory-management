package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class TransferCreateDTO {
    private Long fromLocationId;
    private Long toLocationId;
    private Long createdByUserId;   // NEW
    private List<TransferLineDTO> lines;
}
