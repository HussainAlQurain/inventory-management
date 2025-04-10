package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponseDTO {
    private Long orderId;
    private String orderNumber;
    private LocalDateTime creationDate;
    private LocalDate sentDate;
    private LocalDate deliveryDate;
    private String status;
    private String comments;

    private Long buyerLocationId;
    private String buyerLocationName;

    private Long supplierId;
    private String supplierName;

    // Possibly also createdByUser info, etc.

    // The lines
    private List<OrderItemResponseDTO> items;

}
