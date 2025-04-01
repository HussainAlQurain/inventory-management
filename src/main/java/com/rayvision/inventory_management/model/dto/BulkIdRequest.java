package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.util.Set;

@Data
public class BulkIdRequest {
    private Set<Long> ids;
}
