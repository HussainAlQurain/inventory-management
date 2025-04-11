package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class IntegrationSettingDTO {
    private Long locationId; // which location's setting we're updating or reading

    private String posApiUrl;

    private Integer frequentSyncSeconds;
    private boolean frequentSyncEnabled;

    private boolean dailySyncEnabled;

    // We'll not expose lastFrequentSyncTime or lastDailySyncTime for editing,
    // but we might show them in a response if the user wants to see status
}
