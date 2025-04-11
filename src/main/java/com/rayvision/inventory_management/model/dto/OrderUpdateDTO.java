package com.rayvision.inventory_management.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for updating draft orders
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateDTO {
    private String comments;
    private Long supplierId;
    private List<OrderItemUpdateDTO> updatedItems;
    private List<OrderItemDTO> newItems;
    private List<Long> deletedItemIds;
}