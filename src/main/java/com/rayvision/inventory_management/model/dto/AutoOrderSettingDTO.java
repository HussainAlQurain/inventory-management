package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class AutoOrderSettingDTO {
    private Long locationId;
    private boolean enabled;
    private Integer frequencySeconds;
    private Long systemUserId;
    private String autoOrderComment;
    // We might also show lastCheckTime if you want to display it
}
