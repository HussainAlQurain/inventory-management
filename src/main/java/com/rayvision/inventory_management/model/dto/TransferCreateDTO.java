package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class TransferCreateDTO {
    private Long fromLocationId;
    private Long toLocationId;
    // possibly user ID or other fields

    private List<TransferLineDTO> lines; // each line
}
