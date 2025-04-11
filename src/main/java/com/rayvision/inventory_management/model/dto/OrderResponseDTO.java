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

    private List<OrderItemResponseDTO> items;

    // Added user info
    private Long createdByUserId;
    private String createdByUserName;
}
