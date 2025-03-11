package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.StockTransaction;
import com.rayvision.inventory_management.model.SubRecipe;
import com.rayvision.inventory_management.repository.StockTransactionRepository;
import com.rayvision.inventory_management.service.StockTransactionService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class StockTransactionServiceImpl implements StockTransactionService {

    private final StockTransactionRepository stockTransactionRepository;

    public StockTransactionServiceImpl(StockTransactionRepository stockTransactionRepository) {
        this.stockTransactionRepository = stockTransactionRepository;
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
}
