package com.rayvision.inventory_management.model.dto;

/**
 * Data Transfer Object containing all necessary information for the auto-order process.
 * This eliminates the need for lazy-loaded entity associations in asynchronous operations.
 */
public record ItemOrderInfoDTO(
    Long itemId,
    String itemName,
    Long purchaseOptionId,
    Double price,
    Long supplierId,
    String supplierName
) {}