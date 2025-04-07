package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.StockLevelDTO;
import com.rayvision.inventory_management.repository.InventoryItemRepository;
import com.rayvision.inventory_management.repository.StockTransactionRepository;
import com.rayvision.inventory_management.repository.SubRecipeRepository;
import com.rayvision.inventory_management.service.StockTransactionService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class StockTransactionServiceImpl implements StockTransactionService {

    private final StockTransactionRepository stockTransactionRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SubRecipeRepository subRecipeRepository;
    private final ItemCostCalculator itemCostCalculator;

    public StockTransactionServiceImpl(
            StockTransactionRepository stockTransactionRepository,
            InventoryItemRepository inventoryItemRepository,
            SubRecipeRepository subRecipeRepository,
            ItemCostCalculator itemCostCalculator
    ) {
        this.stockTransactionRepository = stockTransactionRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.subRecipeRepository = subRecipeRepository;
        this.itemCostCalculator = itemCostCalculator;
    }

    // =========================================================================
    // ITEM-based
    // =========================================================================

    @Override
    public StockTransaction recordPurchase(Location location,
                                           InventoryItem item,
                                           Double countQty,
                                           UnitOfMeasure countUom,
                                           Long sourceReferenceId,
                                           LocalDate date) {
        // purchase => +countQty
        return recordTransaction(location, item, null,
                countQty, countUom,
                "PURCHASE", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordUsage(Location location,
                                        InventoryItem item,
                                        Double countQty,
                                        UnitOfMeasure countUom,
                                        Long sourceReferenceId,
                                        LocalDate date) {
        // usage => outflow => negative
        return recordTransaction(location, item, null,
                -countQty, countUom,
                "USAGE", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordTransferIn(Location location,
                                             InventoryItem item,
                                             Double countQty,
                                             UnitOfMeasure countUom,
                                             Long sourceReferenceId,
                                             LocalDate date) {
        // inflow => +countQty
        return recordTransaction(location, item, null,
                countQty, countUom,
                "TRANSFER_IN", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordTransferOut(Location location,
                                              InventoryItem item,
                                              Double countQty,
                                              UnitOfMeasure countUom,
                                              Long sourceReferenceId,
                                              LocalDate date) {
        // outflow => -countQty
        return recordTransaction(location, item, null,
                -countQty, countUom,
                "TRANSFER_OUT", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordAdjustment(Location location,
                                             InventoryItem item,
                                             Double countQty,
                                             UnitOfMeasure countUom,
                                             Long sourceReferenceId,
                                             LocalDate date) {
        // adjustment => sign depends on the user’s input (could be + or -)
        return recordTransaction(location, item, null,
                countQty, countUom,
                "ADJUSTMENT", sourceReferenceId, date);
    }

    // =========================================================================
    // SUBRECIPE-based
    // =========================================================================

    @Override
    public StockTransaction recordPurchase(Location location,
                                           SubRecipe sub,
                                           Double countQty,
                                           UnitOfMeasure countUom,
                                           Long sourceReferenceId,
                                           LocalDate date) {
        return recordTransaction(location, null, sub,
                countQty, countUom,
                "PURCHASE", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordUsage(Location location,
                                        SubRecipe sub,
                                        Double countQty,
                                        UnitOfMeasure countUom,
                                        Long sourceReferenceId,
                                        LocalDate date) {
        return recordTransaction(location, null, sub,
                -countQty, countUom,
                "USAGE", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordTransferIn(Location location,
                                             SubRecipe sub,
                                             Double countQty,
                                             UnitOfMeasure countUom,
                                             Long sourceReferenceId,
                                             LocalDate date) {
        return recordTransaction(location, null, sub,
                countQty, countUom,
                "TRANSFER_IN", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordTransferOut(Location location,
                                              SubRecipe sub,
                                              Double countQty,
                                              UnitOfMeasure countUom,
                                              Long sourceReferenceId,
                                              LocalDate date) {
        return recordTransaction(location, null, sub,
                -countQty, countUom,
                "TRANSFER_OUT", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordAdjustment(Location location,
                                             SubRecipe sub,
                                             Double countQty,
                                             UnitOfMeasure countUom,
                                             Long sourceReferenceId,
                                             LocalDate date) {
        return recordTransaction(location, null, sub,
                countQty, countUom,
                "ADJUSTMENT", sourceReferenceId, date);
    }

    // =========================================================================
    // Core "recordTransaction" method
    // =========================================================================
    private StockTransaction recordTransaction(Location location,
                                               InventoryItem item,
                                               SubRecipe sub,
                                               Double finalCountQty,   // might already be negative for outflow
                                               UnitOfMeasure countUom,
                                               String transactionType,
                                               Long sourceReferenceId,
                                               LocalDate date) {

        StockTransaction tx = new StockTransaction();
        tx.setLocation(location);
        tx.setItem(item);
        tx.setSubRecipe(sub);
        tx.setTransactionType(transactionType);
        tx.setSourceReferenceId(sourceReferenceId);
        tx.setDate((date != null) ? date : LocalDate.now());

        double baseQty;
        double baseCost;

        if (item != null) {
            baseQty = convertQtyToItemBase(item, finalCountQty, countUom);
            baseCost = computeItemCost(item, Math.abs(finalCountQty), countUom);

            // if finalCountQty is negative => negative cost
            if (finalCountQty < 0) {
                baseCost = -baseCost;
            }

        } else if (sub != null) {
            baseQty = convertQtyToSubBase(sub, finalCountQty, countUom);
            baseCost = computeSubCost(sub, Math.abs(finalCountQty), countUom);

            if (finalCountQty < 0) {
                baseCost = -baseCost;
            }

        } else {
            baseQty = 0.0;
            baseCost = 0.0;
        }

        tx.setQuantityChange(baseQty);
        tx.setCostChange(baseCost);

        return stockTransactionRepository.save(tx);
    }

    private double convertQtyToItemBase(InventoryItem item,
                                        double countQty,
                                        UnitOfMeasure countUom) {
        if (item.getInventoryUom() == null) {
            throw new RuntimeException("Item base UOM not set.");
        }
        double ratio = countUom.getConversionFactor() / item.getInventoryUom().getConversionFactor();
        return countQty * ratio;
    }

    private double computeItemCost(InventoryItem item,
                                   double positiveQty,
                                   UnitOfMeasure countUom) {
        // uses your ItemCostCalculator
        return ItemCostCalculator.computeCost(item, positiveQty, countUom);
    }

    private double convertQtyToSubBase(SubRecipe sub,
                                       double countQty,
                                       UnitOfMeasure countUom) {
        if (sub.getUom() == null) {
            throw new RuntimeException("SubRecipe base UOM not set.");
        }
        double ratio = countUom.getConversionFactor() / sub.getUom().getConversionFactor();
        return countQty * ratio;
    }

    private double computeSubCost(SubRecipe sub,
                                  double positiveQty,
                                  UnitOfMeasure countUom) {
        // if you want a separate “SubRecipeCostCalculator” do it here
        if (sub.getUom() == null) {
            return 0.0;
        }
        double ratio = countUom.getConversionFactor() / sub.getUom().getConversionFactor();
        double baseQty = positiveQty * ratio;
        double costPerBase = (sub.getCost() != null) ? sub.getCost() : 0.0;
        return baseQty * costPerBase;
    }

    // =========================================================================
    // Existing Methods
    // =========================================================================
    @Override
    public double calculateTheoreticalOnHand(Long locationId, Long itemId, LocalDate upToDate) {
        List<StockTransaction> txs = stockTransactionRepository
                .findByLocationIdAndItemIdAndDateLessThanEqual(locationId, itemId, upToDate);
        return txs.stream().mapToDouble(StockTransaction::getQuantityChange).sum();
    }

    @Override
    public double calculateTheoreticalOnHandUnified(Long locationId,
                                                    Long itemId,
                                                    Long subRecipeId,
                                                    LocalDate upToDate) {
        if (itemId != null && subRecipeId != null) {
            throw new RuntimeException("Cannot compute theoretical with both itemId and subRecipeId set");
        }
        if (itemId == null && subRecipeId == null) {
            return 0.0;
        }

        if (itemId != null) {
            // item
            List<StockTransaction> txs = stockTransactionRepository
                    .findByLocationAndItemUpToDate(locationId, itemId, upToDate);
            return txs.stream().mapToDouble(StockTransaction::getQuantityChange).sum();
        } else {
            // subRecipe
            List<StockTransaction> txs = stockTransactionRepository
                    .findByLocationAndSubRecipeUpToDate(locationId, subRecipeId, upToDate);
            return txs.stream().mapToDouble(StockTransaction::getQuantityChange).sum();
        }
    }

    @Override
    public void deleteBySourceReferenceId(Long referenceId) {
        List<StockTransaction> txList = stockTransactionRepository.findBySourceReferenceId(referenceId);
        stockTransactionRepository.deleteAll(txList);
    }

    @Override
    public List<StockLevelDTO> getStockLevelsForLocation(Long locationId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.of(1970,1,1);
        if (endDate == null) endDate = LocalDate.now();

        LocalDate openingCutoff = startDate.minusDays(1);
        Map<String, Double> openingMap = buildQuantityMap(locationId, openingCutoff);
        Map<String, Double> closingMap = buildQuantityMap(locationId, endDate);

        List<StockLevelDTO> result = new ArrayList<>();
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(openingMap.keySet());
        allKeys.addAll(closingMap.keySet());

        for (String key : allKeys) {
            double openQty  = openingMap.getOrDefault(key, 0.0);
            double closeQty = closingMap.getOrDefault(key, 0.0);
            double finalQty = closeQty - openQty;  // net base quantity

            if (Math.abs(finalQty) < 0.000001) {
                continue;
            }
            StockLevelDTO dto = new StockLevelDTO();
            dto.setOnHand(finalQty);

            if (key.startsWith("item-")) {
                Long itemId = Long.valueOf(key.substring(5));
                InventoryItem item = inventoryItemRepository.findById(itemId)
                        .orElseThrow(() -> new RuntimeException("Item not found"));
                dto.setItemId(itemId);
                dto.setName(item.getName());
                dto.setUomName(item.getInventoryUom().getName());
                dto.setUomAbbreviation(item.getInventoryUom().getAbbreviation());

                Double costPerBaseUom = item.getCurrentPrice();
                if (costPerBaseUom != null) {
                    dto.setCost(costPerBaseUom * finalQty);
                }
            } else {
                Long subId = Long.valueOf(key.substring(4));
                SubRecipe sub = subRecipeRepository.findById(subId)
                        .orElseThrow(() -> new RuntimeException("Sub not found"));
                dto.setSubRecipeId(subId);
                dto.setName(sub.getName());
                dto.setUomName(sub.getUom().getName());
                dto.setUomAbbreviation(sub.getUom().getAbbreviation());

                Double costPerBase = sub.getCost();
                if (costPerBase != null) {
                    dto.setCost(costPerBase * finalQty);
                }
            }
            result.add(dto);
        }
        return result;
    }

    private Map<String, Double> buildQuantityMap(Long locationId, LocalDate cutoffDate) {
        Map<String, Double> qtyMap = new HashMap<>();
        List<StockTransaction> txs = stockTransactionRepository
                .findAllByLocationIdAndDateLessThanEqual(locationId, cutoffDate);

        for (StockTransaction tx : txs) {
            Long itemId = (tx.getItem() != null) ? tx.getItem().getId() : null;
            Long subId  = (tx.getSubRecipe() != null) ? tx.getSubRecipe().getId() : null;

            String key = (itemId != null)
                    ? "item-" + itemId
                    : "sub-"  + subId;

            double oldQty = qtyMap.getOrDefault(key, 0.0);
            qtyMap.put(key, oldQty + tx.getQuantityChange());
        }
        return qtyMap;
    }

}
