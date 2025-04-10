package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.StockLevelDTO;
import com.rayvision.inventory_management.repository.InventoryItemRepository;
import com.rayvision.inventory_management.repository.StockTransactionRepository;
import com.rayvision.inventory_management.repository.SubRecipeRepository;
import com.rayvision.inventory_management.service.InventoryItemLocationService;
import com.rayvision.inventory_management.service.PurchaseOptionPriceHistoryService;
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
    private final InventoryItemLocationService inventoryItemLocationService;
    private final PurchaseOptionPriceHistoryService priceHistoryService;

    public StockTransactionServiceImpl(
            StockTransactionRepository stockTransactionRepository,
            InventoryItemRepository inventoryItemRepository,
            SubRecipeRepository subRecipeRepository,
            ItemCostCalculator itemCostCalculator,
            InventoryItemLocationService inventoryItemLocationService,
            PurchaseOptionPriceHistoryService priceHistoryService
    ) {
        this.stockTransactionRepository = stockTransactionRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.subRecipeRepository = subRecipeRepository;
        this.itemCostCalculator = itemCostCalculator;
        this.inventoryItemLocationService = inventoryItemLocationService;
        this.priceHistoryService = priceHistoryService;
    }

    // =========================================================================
    // ITEM-based
    // =========================================================================

    @Override
    public StockTransaction recordPurchase(Location location,
                                           InventoryItem item,
                                           Double qty,
                                           UnitOfMeasure uom,
                                           Long sourceReferenceId,
                                           LocalDate date,
                                           Double overridePrice,
                                           boolean updateOptionPrice) {
        return recordTransaction(location, item, null,
                qty, uom,
                "PURCHASE", sourceReferenceId, date,
                overridePrice, updateOptionPrice
        );
    }

    @Override
    public StockTransaction recordPurchase(Location location,
                                           InventoryItem item,
                                           Double qty,
                                           UnitOfMeasure uom,
                                           Long sourceReferenceId,
                                           LocalDate date) {
        // call the new method with overridePrice=null
        return recordPurchase(location, item, qty, uom, sourceReferenceId, date, null, false);
    }

    @Override
    public StockTransaction recordUsage(Location location,
                                        InventoryItem item,
                                        Double qty,
                                        UnitOfMeasure uom,
                                        Long sourceReferenceId,
                                        LocalDate date) {
        return recordTransaction(location, item, null,
                -qty, uom,
                "USAGE", sourceReferenceId, date,
                null, false);
    }


    @Override
    public StockTransaction recordTransferIn(Location location,
                                             InventoryItem item,
                                             Double qty,
                                             UnitOfMeasure uom,
                                             Long sourceReferenceId,
                                             LocalDate date) {
        return recordTransaction(location, item, null,
                qty, uom,
                "TRANSFER_IN", sourceReferenceId, date,
                null, false);
    }


    @Override
    public StockTransaction recordTransferOut(Location location,
                                              InventoryItem item,
                                              Double qty,
                                              UnitOfMeasure uom,
                                              Long sourceReferenceId,
                                              LocalDate date) {
        return recordTransaction(location, item, null,
                -qty, uom,
                "TRANSFER_OUT", sourceReferenceId, date,
                null, false);
    }


    @Override
    public StockTransaction recordAdjustment(Location location,
                                             InventoryItem item,
                                             Double qty,
                                             UnitOfMeasure uom,
                                             Long sourceReferenceId,
                                             LocalDate date) {
        return recordTransaction(location, item, null,
                qty, uom,
                "ADJUSTMENT", sourceReferenceId, date,
                null, false);
    }


    // =========================================================================
    // SUBRECIPE-based
    // =========================================================================

    @Override
    public StockTransaction recordPurchase(Location location,
                                           SubRecipe sub,
                                           Double qty,
                                           UnitOfMeasure uom,
                                           Long sourceReferenceId,
                                           LocalDate date) {
        return recordTransaction(location, null, sub,
                qty, uom,
                "PURCHASE", sourceReferenceId, date,
                null, false);
    }

    @Override
    public StockTransaction recordUsage(Location location,
                                        SubRecipe sub,
                                        Double qty,
                                        UnitOfMeasure uom,
                                        Long sourceReferenceId,
                                        LocalDate date) {
        return recordTransaction(location, null, sub,
                -qty, uom,
                "USAGE", sourceReferenceId, date,
                null, false);
    }

    @Override
    public StockTransaction recordTransferIn(Location location,
                                             SubRecipe sub,
                                             Double qty,
                                             UnitOfMeasure uom,
                                             Long sourceReferenceId,
                                             LocalDate date) {
        return recordTransaction(location, null, sub,
                qty, uom,
                "TRANSFER_IN", sourceReferenceId, date,
                null, false);
    }

    @Override
    public StockTransaction recordTransferOut(Location location,
                                              SubRecipe sub,
                                              Double qty,
                                              UnitOfMeasure uom,
                                              Long sourceReferenceId,
                                              LocalDate date) {
        return recordTransaction(location, null, sub,
                -qty, uom,
                "TRANSFER_OUT", sourceReferenceId, date,
                null, false);
    }

    @Override
    public StockTransaction recordAdjustment(Location location,
                                             SubRecipe sub,
                                             Double qty,
                                             UnitOfMeasure uom,
                                             Long sourceReferenceId,
                                             LocalDate date) {
        return recordTransaction(location, null, sub,
                qty, uom,
                "ADJUSTMENT", sourceReferenceId, date,
                null, false);
    }


    // =========================================================================
    // Core "recordTransaction" method
    // =========================================================================
    private StockTransaction recordTransaction(Location location,
                                               InventoryItem item,
                                               SubRecipe sub,
                                               Double finalCountQty, // might be negative
                                               UnitOfMeasure countUom,
                                               String transactionType,
                                               Long sourceReferenceId,
                                               LocalDate date,
                                               Double overridePrice,
                                               boolean updateOptionPrice) {

        if (finalCountQty == null) finalCountQty = 0.0;

        StockTransaction tx = new StockTransaction();
        tx.setLocation(location);
        tx.setItem(item);
        tx.setSubRecipe(sub);
        tx.setTransactionType(transactionType);
        tx.setSourceReferenceId(sourceReferenceId);
        tx.setDate((date != null) ? date : LocalDate.now());

        double baseQty = 0.0;
        double baseCost = 0.0;

        if (item != null) {
            // ITEM logic
            baseQty = convertQtyToItemBase(item, finalCountQty, countUom);

            // If we have an override price, use it; else compute from ItemCostCalculator
            if (overridePrice != null) {
                // cost in base
                double positiveBaseQty = Math.abs(baseQty);
                double costInBase = positiveBaseQty * overridePrice;
                baseCost = costInBase;
                if (baseQty < 0) {
                    baseCost = -baseCost;
                }
            } else {
                // fallback
                double cost = itemCostCalculator.computeCost(item, Math.abs(finalCountQty), countUom);
                if (finalCountQty < 0) {
                    cost = -cost;
                }
                baseCost = cost;
            }

        } else if (sub != null) {
            // SubRecipe logic
            baseQty = convertQtyToSubBase(sub, finalCountQty, countUom);
            double cost = computeSubCost(sub, Math.abs(finalCountQty), countUom);
            if (finalCountQty < 0) {
                cost = -cost;
            }
            baseCost = cost;
        }

        // Save into transaction
        tx.setQuantityChange(baseQty);
        tx.setCostChange(baseCost);
        StockTransaction savedTx = stockTransactionRepository.save(tx);

        // 1) Update bridging if it's an item
        if (item != null && Math.abs(baseQty) > 0.000001) {
            // if it's an inflow => baseQty>0 => increment
            // outflow => baseQty<0 => increment with negative
            inventoryItemLocationService.incrementOnHand(item.getId(), location.getId(), baseQty);
        }

        // 2) If "PURCHASE" of an item with finalCountQty>0 => update item’s running average cost
        if ("PURCHASE".equalsIgnoreCase(transactionType) && item != null && finalCountQty > 0) {
            double positiveBaseQty = Math.abs(baseQty);
            double finalUnitCost = baseCost / baseQty; // costChange/baseQty
            updateItemRunningAverageCost(item, positiveBaseQty, finalUnitCost);

            // 3) If user wants to update the purchaseOption price
            if (updateOptionPrice && overridePrice != null) {
                // we find the purchaseoption and record the new price
                // (this part depends on how you link item->supplier->purchaseoption)
                PurchaseOption po = pickMainOrFallbackPurchaseOption(item);
                if (po != null && !po.getPrice().equals(overridePrice)) {
                    // record new price in price history + set it
                    priceHistoryService.recordPriceChange(po, overridePrice);
                }
            }
        }

        return savedTx;
    }

    // ----------------------------------------------------------------------
    // Running-average on the item
    // item.cumulativeQty, item.cumulativeValue
    // ----------------------------------------------------------------------
    private void updateItemRunningAverageCost(InventoryItem item, double qtyReceivedInBase, double finalUnitCost) {
        double oldQty = (item.getCumulativeQty() != null) ? item.getCumulativeQty() : 0.0;
        double oldVal = (item.getCumulativeValue() != null) ? item.getCumulativeValue() : 0.0;

        double addValue = qtyReceivedInBase * finalUnitCost;
        double newVal   = oldVal + addValue;
        double newQty   = oldQty + qtyReceivedInBase;

        if (Math.abs(newQty) < 0.000001) {
            item.setCumulativeQty(0.0);
            item.setCumulativeValue(0.0);
            item.setCurrentPrice(finalUnitCost);
        } else {
            double avg = newVal / newQty;
            item.setCumulativeQty(newQty);
            item.setCumulativeValue(newVal);
            item.setCurrentPrice(avg);
        }
        inventoryItemRepository.save(item);
    }

    // ----------------------------------------------------------------------
    // Convert item qty to base
    // ----------------------------------------------------------------------
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

    // Example method to pick the main or fallback purchase option from the item
    private PurchaseOption pickMainOrFallbackPurchaseOption(InventoryItem item) {
        if (item.getPurchaseOptions() == null || item.getPurchaseOptions().isEmpty()) {
            return null;
        }
        // find main
        Optional<PurchaseOption> mainOpt = item.getPurchaseOptions().stream()
                .filter(PurchaseOption::isMainPurchaseOption)
                .findFirst();
        if (mainOpt.isPresent()) {
            return mainOpt.get();
        }
        // else first
        return item.getPurchaseOptions().iterator().next();
    }


}
