package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class TransferCreateDTO {
    private Long fromLocationId;
    private Long toLocationId;
    // possibly user ID or other fields

    private List<TransferLineDTO> lines; // each line
}
