package com.rayvision.inventory_management.model.dto;

/**
 * Data Transfer Object containing all necessary information for the auto-order process.
 * This eliminates the need for lazy-loaded entity associations in asynchronous operations.
 * Enhanced to include inventory location data to reduce database queries.
 */
public record ItemOrderInfoDTO(
    // Item data
    Long itemId,
    String itemName,
    
    // Purchase option data
    Long purchaseOptionId,
    Double price,
    Boolean mainPurchaseOption,
    
    // Supplier data
    Long supplierId,
    String supplierName,
    
    // Inventory location data
    Double onHand,
    Double minOnHand,
    Double parLevel,
    
    // Optional in-transit quantity (may be null)
    Double inTransitQty
) {
    /**
     * Helper method to get effective on-hand quantity (on-hand + in-transit)
     */
    public Double getEffectiveOnHand() {
        double onHandValue = onHand != null ? onHand : 0.0;
        double inTransitValue = inTransitQty != null ? inTransitQty : 0.0;
        return onHandValue + inTransitValue;
    }
    
    /**
     * Helper method to calculate shortage based on PAR and effective on-hand
     */
    public Double calculateShortage() {
        if (parLevel == null || parLevel <= 0) {
            return 0.0;
        }
        
        double effective = getEffectiveOnHand();
        double shortage = parLevel - effective;
        
        return Math.max(0.0, shortage);
    }
    
    /**
     * Check if this item needs ordering (has a valid PAR and is below it)
     */
    public boolean needsOrdering() {
        return parLevel != null && parLevel > 0 && calculateShortage() > 0;
    }
}