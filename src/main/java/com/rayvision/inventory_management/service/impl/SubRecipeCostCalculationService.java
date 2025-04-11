package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.SubRecipe;
import com.rayvision.inventory_management.model.SubRecipeLine;
import com.rayvision.inventory_management.model.UnitOfMeasure;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class SubRecipeCostCalculationService {
    public double calculateLineCost(SubRecipeLine line) {
        double lineCost = 0.0;
        double netQty = line.getQuantity() != null ? line.getQuantity() : 0.0;
        double wastage = line.getWastagePercent() != null ? (line.getWastagePercent() / 100) : 0.0;
        double grossQty = netQty * (1.0 + wastage);

        if (line.getInventoryItem() != null) {
            // InventoryItem cost calculation
            InventoryItem item = line.getInventoryItem();
            UnitOfMeasure lineUom = line.getUnitOfMeasure();
            UnitOfMeasure itemUom = item.getInventoryUom();

            validateUOMs(lineUom, itemUom);

            double convertedQty = grossQty * (lineUom.getConversionFactor() / itemUom.getConversionFactor());
            lineCost = convertedQty * item.getCurrentPrice();

        } else if (line.getChildSubRecipe() != null) {
            // Child SubRecipe cost calculation
            SubRecipe child = line.getChildSubRecipe();
            UnitOfMeasure lineUom = line.getUnitOfMeasure();
            UnitOfMeasure childUom = child.getUom();

            validateUOMs(lineUom, childUom);

            double convertedQty = grossQty * (lineUom.getConversionFactor() / childUom.getConversionFactor());
            double childYield = child.getYieldQty() != null ? child.getYieldQty() : 1.0;

            lineCost = (convertedQty / childYield) * child.getCost();
        }

        return lineCost;
    }

    private void validateUOMs(UnitOfMeasure uom1, UnitOfMeasure uom2) {
        if (uom1 == null || uom2 == null) {
            throw new IllegalStateException("UOMs must be specified");
        }
        if (uom1.getConversionFactor() <= 0 || uom2.getConversionFactor() <= 0) {
            throw new IllegalStateException("UOM conversion factors must be positive");
        }
    }

    @Transactional
    public void recalculateSubRecipeCost(SubRecipe subRecipe) {
        double totalCost = subRecipe.getSubRecipeLines().stream()
                .mapToDouble(line -> {
                    double lineCost = calculateLineCost(line);
                    line.setLineCost(lineCost); // Update line cost
                    return lineCost;
                })
                .sum();

        subRecipe.setCost(totalCost);
    }

}
