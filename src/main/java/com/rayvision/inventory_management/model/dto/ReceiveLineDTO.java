package com.rayvision.inventory_management.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Partial receiving line overrides:
 *  - orderItemId: which line to update
 *  - receivedQty: how many units actually received
 *  - finalPrice: new cost each (if different from the original)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceiveLineDTO {
    private Long orderItemId;    // ID of the line being received
    private Double receivedQty;  // how many units we actually got
    private Double finalPrice;   // cost each (if updated from the PO)
    // You could add “creditMemo” or “extraFees” fields as well
}
