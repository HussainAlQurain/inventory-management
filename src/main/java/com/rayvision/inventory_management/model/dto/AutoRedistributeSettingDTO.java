package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class AutoRedistributeSettingDTO {
    private Boolean enabled;           // true / false
    private Integer frequencySeconds;  // ≥ 60       (null ⇒ leave unchanged)
    private String  autoTransferComment;
}
