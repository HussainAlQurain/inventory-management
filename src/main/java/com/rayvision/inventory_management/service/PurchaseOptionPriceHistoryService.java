package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.PurchaseOption;

public interface PurchaseOptionPriceHistoryService {
    void recordPriceChange(PurchaseOption po, Double newPrice);
}
