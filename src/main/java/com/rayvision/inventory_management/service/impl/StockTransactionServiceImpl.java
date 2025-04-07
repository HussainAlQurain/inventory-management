package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.StockTransaction;
import com.rayvision.inventory_management.model.SubRecipe;
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

    public StockTransactionServiceImpl(StockTransactionRepository stockTransactionRepository, InventoryItemRepository inventoryItemRepository, SubRecipeRepository subRecipeRepository) {
        this.stockTransactionRepository = stockTransactionRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.subRecipeRepository = subRecipeRepository;
    }

    // -------------------------------------------------------------------------
    // ITEM-based methods (existing)
    // -------------------------------------------------------------------------
    @Override
    public StockTransaction recordPurchase(Location location,
                                           InventoryItem item,
                                           Double qty,
                                           Double cost,
                                           Long sourceReferenceId,
                                           LocalDate date) {
        return recordTransaction(location, item, null, qty, cost, "PURCHASE", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordTransferIn(Location location,
                                             InventoryItem item,
                                             Double qty,
                                             Double cost,
                                             Long sourceReferenceId,
                                             LocalDate date) {
        return recordTransaction(location, item, null, qty, cost, "TRANSFER_IN", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordTransferOut(Location location,
                                              InventoryItem item,
                                              Double qty,
                                              Double cost,
                                              Long sourceReferenceId,
                                              LocalDate date) {
        // negative for outflow
        return recordTransaction(location, item, null, -qty, -cost, "TRANSFER_OUT", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordUsage(Location location,
                                        InventoryItem item,
                                        Double qty,
                                        Double cost,
                                        Long sourceReferenceId,
                                        LocalDate date) {
        return recordTransaction(location, item, null, -qty, -cost, "USAGE", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordAdjustment(Location location,
                                             InventoryItem item,
                                             Double qty,
                                             Double cost,
                                             Long sourceReferenceId,
                                             LocalDate date) {
        return recordTransaction(location, item, null, qty, cost, "ADJUSTMENT", sourceReferenceId, date);
    }

    // -------------------------------------------------------------------------
    // SUBRECIPE-based methods (NEW)
    // -------------------------------------------------------------------------
    @Override
    public StockTransaction recordPurchase(Location location,
                                           SubRecipe sub,
                                           Double qty,
                                           Double cost,
                                           Long sourceReferenceId,
                                           LocalDate date) {
        return recordTransaction(location, null, sub, qty, cost, "PURCHASE", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordTransferIn(Location location,
                                             SubRecipe sub,
                                             Double qty,
                                             Double cost,
                                             Long sourceReferenceId,
                                             LocalDate date) {
        return recordTransaction(location, null, sub, qty, cost, "TRANSFER_IN", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordTransferOut(Location location,
                                              SubRecipe sub,
                                              Double qty,
                                              Double cost,
                                              Long sourceReferenceId,
                                              LocalDate date) {
        return recordTransaction(location, null, sub, -qty, -cost, "TRANSFER_OUT", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordUsage(Location location,
                                        SubRecipe sub,
                                        Double qty,
                                        Double cost,
                                        Long sourceReferenceId,
                                        LocalDate date) {
        return recordTransaction(location, null, sub, -qty, -cost, "USAGE", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordAdjustment(Location location,
                                             SubRecipe sub,
                                             Double qty,
                                             Double cost,
                                             Long sourceReferenceId,
                                             LocalDate date) {
        return recordTransaction(location, null, sub, qty, cost, "ADJUSTMENT", sourceReferenceId, date);
    }

    // -------------------------------------------------------------------------
    // A single private method that can handle item or subRecipe
    // -------------------------------------------------------------------------
    private StockTransaction recordTransaction(Location location,
                                               InventoryItem item,
                                               SubRecipe sub,
                                               Double qtyChange,
                                               Double costChange,
                                               String transactionType,
                                               Long sourceReferenceId,
                                               LocalDate date) {
        StockTransaction tx = new StockTransaction();
        tx.setLocation(location);
        tx.setItem(item);  // might be null if subRecipe is used
        tx.setSubRecipe(sub); // might be null if item is used
        tx.setQuantityChange(qtyChange);
        tx.setCostChange(costChange);
        tx.setTransactionType(transactionType);
        tx.setSourceReferenceId(sourceReferenceId);
        tx.setDate(date != null ? date : LocalDate.now());

        return stockTransactionRepository.save(tx);
    }

    // -------------------------------------------------------------------------
    // For item-based back-compat
    // -------------------------------------------------------------------------
    @Override
    public double calculateTheoreticalOnHand(Long locationId, Long itemId, LocalDate upToDate) {
        // your existing logic
        List<StockTransaction> txs = stockTransactionRepository
                .findByLocationIdAndItemIdAndDateLessThanEqual(locationId, itemId, upToDate);
        return txs.stream()
                .mapToDouble(StockTransaction::getQuantityChange)
                .sum();
    }

    // -------------------------------------------------------------------------
    // NEW: handle EITHER item or subRecipe
    // -------------------------------------------------------------------------
    @Override
    public double calculateTheoreticalOnHandUnified(Long locationId,
                                                    Long itemId,
                                                    Long subRecipeId,
                                                    LocalDate upToDate) {
        if (itemId != null && subRecipeId != null) {
            throw new RuntimeException("Cannot compute theoretical with both itemId and subRecipeId set");
        }
        if (itemId == null && subRecipeId == null) {
            return 0.0; // or throw
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
    public List<StockLevelDTO> getStockLevelsForLocation(Long locationId,
                                                         LocalDate startDate,
                                                         LocalDate endDate) {
        // 1) If null checks:
        if (startDate == null) {
            // e.g. set a default earliest date
            startDate = LocalDate.of(1970,1,1);
        }
        if (endDate == null) {
            // default to “today”
            endDate = LocalDate.now();
        }

        // 2) Build a map of "opening" sums = transactions up to startDate.minusDays(1)
        LocalDate openingCutoff = startDate.minusDays(1);
        Map<String, Double> openingMap = buildQuantityMap(locationId, openingCutoff);

        // 3) Build a map of "closing" sums = transactions up to endDate
        Map<String, Double> closingMap = buildQuantityMap(locationId, endDate);

        // 4) For each key in closingMap, subtract the opening. Then create a StockLevelDTO.
        //    Key is "item-123" or "sub-456"
        List<StockLevelDTO> result = new ArrayList<>();

        // We'll union the keys from both maps so we don't miss any
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(openingMap.keySet());
        allKeys.addAll(closingMap.keySet());

        for (String key : allKeys) {
            double openQty  = openingMap.getOrDefault(key, 0.0);
            double closeQty = closingMap.getOrDefault(key, 0.0);

            // The final on-hand for that period is (closeQty - openQty).
            double finalQty = closeQty - openQty;
            if (Math.abs(finalQty) < 0.000001) {
                // effectively zero; skip it if you want to filter out zero stocks
                continue;
            }

            StockLevelDTO dto = new StockLevelDTO();
            dto.setOnHand(finalQty);

            if (key.startsWith("item-")) {
                Long itemId = Long.valueOf(key.substring(5));
                InventoryItem item = inventoryItemRepository.findById(itemId)
                        .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));
                dto.setItemId(itemId);
                dto.setName(item.getName());
                // If you want cost:
                // dto.setCost(item.getCurrentPrice());
            } else {
                Long subId = Long.valueOf(key.substring(4));
                SubRecipe sub = subRecipeRepository.findById(subId)
                        .orElseThrow(() -> new RuntimeException("SubRecipe not found: " + subId));
                dto.setSubRecipeId(subId);
                dto.setName(sub.getName());
                // If you want cost:
                // dto.setCost(sub.getCost());
            }

            result.add(dto);
        }

        return result;
    }

    /**
     * Sums all StockTransaction.quantityChange for a given location
     * up to the given cutoff date, grouping by (itemId or subRecipeId).
     * Returns a map: "item-123" -> sum, or "sub-456" -> sum.
     */
    private Map<String, Double> buildQuantityMap(Long locationId, LocalDate cutoffDate) {
        Map<String, Double> qtyMap = new HashMap<>();

        // fetch all transactions up to cutoffDate
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
