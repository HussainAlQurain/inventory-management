package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class StorageAreaDTO {
    private Long id;          // optional if client is editing an existing record
    private String name;
    private String description;
    private Long locationId;  // reference to an existing Location
}
