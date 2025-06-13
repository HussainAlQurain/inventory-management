package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TransferDTO {
    private Long id;
    private LocalDate creationDate;
    private LocalDate sentDate;        // NEW
    private LocalDate deliveryDate;    // NEW
    private LocalDate completionDate;
    private String status;
    private Long createdByUserId;      // NEW

    private Long fromLocationId;
    private String fromLocationName;
    private Long toLocationId;
    private String toLocationName;

    private List<TransferLineDTO> lines;
}
