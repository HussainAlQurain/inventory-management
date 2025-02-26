package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderCreateDTO {

    private Long buyerLocationId;    // the location receiving the goods
    private Long supplierId;         // the supplier
    private Long createdByUserId;    // who is creating the order
    private String comments;

    private List<OrderItemDTO> items; // each item the user wants to buy

}
