package com.rayvision.inventory_management.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for updating existing order items
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemUpdateDTO {
    private Long orderItemId;
    private Double quantity;
    private Double price;
    private Long uomId;
}