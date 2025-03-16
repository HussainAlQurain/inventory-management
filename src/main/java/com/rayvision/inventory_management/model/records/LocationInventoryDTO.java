package com.rayvision.inventory_management.model.records;

public record LocationInventoryDTO(
        Long locationId,
        String locationName,
        double quantity,
        double value
) {}

