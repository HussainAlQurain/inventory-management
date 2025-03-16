package com.rayvision.inventory_management.model.records;

public record BulkUpdateRequest(Long companyId, Long itemId, Double newMin, Double newPar) {}

