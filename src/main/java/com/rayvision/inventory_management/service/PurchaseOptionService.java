package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.PurchaseOption;
import com.rayvision.inventory_management.model.dto.PurchaseOptionCreateDTO;
import com.rayvision.inventory_management.model.dto.PurchaseOptionPartialUpdateDTO;

import java.util.List;

public interface PurchaseOptionService {
    PurchaseOption createPurchaseOption(Long companyId, Long inventoryItemId, PurchaseOptionCreateDTO dto);
    PurchaseOption partialUpdate(Long companyId, Long purchaseOptionId, PurchaseOptionPartialUpdateDTO dto);
    void disablePurchaseOption(Long companyId, Long purchaseOptionId);
    PurchaseOption updatePriceManually(Long companyId, Long purchaseOptionId, Double newPrice);
    List<PurchaseOption> getPurchaseOptions(Long inventoryItemId);
}
