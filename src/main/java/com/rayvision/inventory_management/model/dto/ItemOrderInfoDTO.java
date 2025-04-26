package com.rayvision.inventory_management.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class ItemOrderInfoDTO {
    // Item information
    private Long itemId;
    private String itemName;
    
    // Purchase option information
    private Long purchaseOptionId;
    private Double price;
    private Boolean isMainOption;
    
    // UOM information
    private String inventoryUom;
    private String purchaseUom;
    private Double conversionFactor; // Multiplier to convert from inventory UOM to purchase UOM
    
    // Supplier packaging information
    private Double orderMultipleQty = 1.0; // Minimum order quantity or packaging unit
    
    // Supplier information
    private Long supplierId;
    private String supplierName;
    
    // Quantity information
    private Double onHand;
    private Double minOnHand;
    private Double parLevel;
    private Double inTransitQty;
    private Double inDraftOrdersQty;
    private Double inPendingTransfersQty;
    
    /**
     * Determines if this item needs ordering based on inventory levels and par levels.
     */
    public boolean needsOrdering() {
        // No need to order if min/par levels aren't set
        if (minOnHand == null || parLevel == null) {
            if (log.isDebugEnabled()) {
                log.debug("Item {} ({}) doesn't need ordering: minOnHand={}, parLevel={}",
                        itemName, itemId, minOnHand, parLevel);
            }
            return false;
        }
        
        // Calculate effective quantity including in-transit and pending quantities
        double effectiveQty = calculateEffectiveQuantity();
        
        // Only order if we're below the minimum level
        boolean needsOrder = effectiveQty < minOnHand;
        
        if (log.isDebugEnabled()) {
            log.debug("Item {} ({}): effectiveQty={}, minOnHand={}, needsOrdering={}",
                    itemName, itemId, effectiveQty, minOnHand, needsOrder);
        }
        
        return needsOrder;
    }
    
    /**
     * Calculates the effective quantity considering all sources
     */
    public double calculateEffectiveQuantity() {
        // Default any null values to zero to avoid NPEs
        double onHandQty = onHand != null ? onHand : 0.0;
        double inTransit = inTransitQty != null ? inTransitQty : 0.0;
        double inDraft = inDraftOrdersQty != null ? inDraftOrdersQty : 0.0; 
        double inPending = inPendingTransfersQty != null ? inPendingTransfersQty : 0.0;
        
        // Log quantities for debugging
        if (log.isDebugEnabled()) {
            log.debug("Item: {} ({}), On-hand: {}, In-transit: {}, In draft: {}, Pending transfers: {}",
                    itemName, itemId, onHandQty, inTransit, inDraft, inPending);
        }
        
        double effectiveQty = onHandQty + inTransit + inDraft + inPending;
        
        if (log.isDebugEnabled()) {
            log.debug("Effective quantity for item {} ({}): {} ({} + {} + {} + {})",
                    itemName, itemId, effectiveQty, onHandQty, inTransit, inDraft, inPending);
        }
        
        return effectiveQty;
    }
    
    /**
     * Calculate how much to order in the inventory UOM units.
     */
    public double calculateShortage() {
        if (minOnHand == null || parLevel == null) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot calculate shortage for item {} ({}): minOnHand={}, parLevel={}",
                        itemName, itemId, minOnHand, parLevel);
            }
            return 0.0;
        }
        
        // Calculate effective quantity including in-transit and pending quantities
        double effectiveQty = calculateEffectiveQuantity();
        
        // If we're at or above minimum, no need to order
        if (effectiveQty >= minOnHand) {
            if (log.isDebugEnabled()) {
                log.debug("No shortage for item {} ({}): effectiveQty={} >= minOnHand={}",
                        itemName, itemId, effectiveQty, minOnHand);
            }
            return 0.0;
        }
        
        // Order enough to reach the par level
        double shortage = parLevel - effectiveQty;
        
        log.info("Calculated shortage for item {} ({}): {}", itemName, itemId, shortage);
        return shortage;
    }
    
    /**
     * Calculate how much to order in the purchase UOM units.
     * Results are rounded up to the nearest order multiple quantity.
     */
    public double calculatePurchaseQuantity() {
        double inventoryUomQty = calculateShortage();
        if (inventoryUomQty <= 0) {
            return 0.0;
        }
        
        // Validate UOM configuration before proceeding
        if (!hasValidUomConfiguration()) {
            log.warn("Skipping purchase quantity calculation for item {} ({}) due to invalid UOM configuration", 
                    itemName, itemId);
            return 0.0;
        }
        
        double purchaseQty;
        
        // Convert from inventory UOM to purchase UOM using the conversion factor
        if (conversionFactor != null && conversionFactor > 0) {
            purchaseQty = inventoryUomQty / conversionFactor;
            
            if (log.isDebugEnabled()) {
                log.debug("Converting {} {} to {} {}: {} / {} = {}",
                        inventoryUomQty, inventoryUom, purchaseUom, 
                        purchaseUom, inventoryUomQty, conversionFactor, purchaseQty);
            }
        } else {
            // If no conversion factor or invalid value, issue a warning and use 1:1 conversion
            log.warn("Invalid or missing conversion factor for item {} ({}): '{}', using 1:1 conversion",
                    itemName, itemId, conversionFactor);
            purchaseQty = inventoryUomQty;
        }
        
        // Round up to the next order multiple quantity
        if (orderMultipleQty != null && orderMultipleQty > 0 && orderMultipleQty != 1.0) {
            double originalQty = purchaseQty;
            purchaseQty = Math.ceil(purchaseQty / orderMultipleQty) * orderMultipleQty;
            
            if (log.isDebugEnabled()) {
                log.debug("Rounding purchase quantity from {} to {} (order multiple: {})",
                        originalQty, purchaseQty, orderMultipleQty);
            }
        } else if (orderMultipleQty == null || orderMultipleQty <= 0) {
            // Log warning if invalid order multiple quantity
            log.warn("Invalid order multiple quantity for item {} ({}): '{}', using no rounding",
                    itemName, itemId, orderMultipleQty);
        }
        
        // Use BigDecimal for precise rounding to 2 decimal places to avoid floating point issues
        BigDecimal bd = BigDecimal.valueOf(purchaseQty);
        bd = bd.setScale(2, RoundingMode.CEILING);
        
        double finalQty = bd.doubleValue();
        
        log.info("Final purchase quantity for item {} ({}): {} {} (from {} {})",
                itemName, itemId, finalQty, purchaseUom, inventoryUomQty, inventoryUom);
        
        return finalQty;
    }
    
    /**
     * Validates that this item has valid UOM configuration for auto-ordering.
     * @return true if the UOM configuration is valid, false otherwise
     */
    public boolean hasValidUomConfiguration() {
        boolean valid = true;
        
        // Check for missing UOMs
        if (inventoryUom == null || inventoryUom.trim().isEmpty()) {
            log.warn("Missing inventory UOM for item {} ({})", itemName, itemId);
            valid = false;
        }
        
        if (purchaseUom == null || purchaseUom.trim().isEmpty()) {
            log.warn("Missing purchase UOM for item {} ({})", itemName, itemId);
            valid = false;
        }
        
        // Check for missing or invalid conversion factor
        if (conversionFactor == null || conversionFactor <= 0) {
            log.warn("Invalid conversion factor '{}' for item {} ({})", 
                    conversionFactor, itemName, itemId);
            valid = false;
        }
        
        return valid;
    }
    
    /**
     * Converts a quantity from purchase UOM to inventory UOM.
     * This is useful for understanding how many inventory units are being ordered.
     *
     * @param purchaseQuantity The quantity in purchase UOM
     * @return The equivalent quantity in inventory UOM
     */
    public double convertPurchaseToInventoryQuantity(double purchaseQuantity) {
        if (purchaseQuantity <= 0) {
            return 0.0;
        }
        
        if (conversionFactor == null || conversionFactor <= 0) {
            log.warn("Invalid conversion factor for item {} ({}): '{}', using 1:1 conversion",
                    itemName, itemId, conversionFactor);
            return purchaseQuantity;
        }
        
        double inventoryQty = purchaseQuantity * conversionFactor;
        
        if (log.isDebugEnabled()) {
            log.debug("Converting {} {} to {} {}: {} * {} = {}",
                    purchaseQuantity, purchaseUom, inventoryUom, 
                    inventoryUom, purchaseQuantity, conversionFactor, inventoryQty);
        }
        
        return inventoryQty;
    }
}