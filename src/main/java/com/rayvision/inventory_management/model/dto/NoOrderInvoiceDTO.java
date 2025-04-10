package com.rayvision.inventory_management.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NoOrderInvoiceDTO {
    private Long locationId;
    private Long userId;
    private Long supplierId;   // optional, can be null
    private String comments;
    private List<NoOrderLineDTO> lines;
}
