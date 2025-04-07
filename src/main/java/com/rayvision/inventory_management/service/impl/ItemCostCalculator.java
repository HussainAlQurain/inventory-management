package com.rayvision.inventory_management.service.impl;

import org.springframework.stereotype.Service;

@Service
public class ItemCostCalculator {
    /**
     * Return the cost for quantity `countQty` in `countUom`,
     * given that item has a "base" inventoryUom + currentPrice.
     *
     * The item’s price is "cost per 1.0 base unit" (where base= item.getInventoryUom()).
     */
    public static double computeCost(InventoryItem item,
                                     double countQty,
                                     UnitOfMeasure countUom) {
        if (item == null || item.getInventoryUom() == null) {
            throw new RuntimeException("Item or item’s base UOM is null.");
        }

        UnitOfMeasure itemUom = item.getInventoryUom();
        validateUOMs(countUom, itemUom);

        double baseQty = countQty * (countUom.getConversionFactor() / itemUom.getConversionFactor());

        double pricePerBaseUom = Optional.ofNullable(item.getCurrentPrice()).orElse(0.0);
        return baseQty * pricePerBaseUom;
    }

    private static void validateUOMs(UnitOfMeasure uom1, UnitOfMeasure uom2) {
        if (uom1 == null || uom2 == null) {
            throw new IllegalStateException("UOMs must be specified");
        }
        if (uom1.getConversionFactor() <= 0 || uom2.getConversionFactor() <= 0) {
            throw new IllegalStateException("UOM conversion factors must be positive");
        }
    }

}
