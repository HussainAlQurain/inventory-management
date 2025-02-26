package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.StockTransaction;
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

    @Override
    public StockTransaction recordPurchase(Location location,
                                           InventoryItem item,
                                           Double qty,
                                           Double cost,
                                           Long sourceReferenceId,
                                           LocalDate date) {
        return recordTransaction(location, item, qty, cost, "PURCHASE", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordTransferIn(Location location,
                                             InventoryItem item,
                                             Double qty,
                                             Double cost,
                                             Long sourceReferenceId,
                                             LocalDate date) {
        return recordTransaction(location, item, qty, cost, "TRANSFER_IN", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordTransferOut(Location location,
                                              InventoryItem item,
                                              Double qty,
                                              Double cost,
                                              Long sourceReferenceId,
                                              LocalDate date) {
        // negative for outflow
        return recordTransaction(location, item, -qty, -cost, "TRANSFER_OUT", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordUsage(Location location,
                                        InventoryItem item,
                                        Double qty,
                                        Double cost,
                                        Long sourceReferenceId,
                                        LocalDate date) {
        // negative for usage
        return recordTransaction(location, item, -qty, -cost, "USAGE", sourceReferenceId, date);
    }

    @Override
    public StockTransaction recordAdjustment(Location location,
                                             InventoryItem item,
                                             Double qty,
                                             Double cost,
                                             Long sourceReferenceId,
                                             LocalDate date) {
        return recordTransaction(location, item, qty, cost, "ADJUSTMENT", sourceReferenceId, date);
    }

    /**
     * The main logic for summing transactions up to a certain date.
     * This yields the "theoretical on-hand" for that item at that location.
     */
    @Override
    public double calculateTheoreticalOnHand(Long locationId, Long itemId, LocalDate upToDate) {
        // fetch all transactions up to 'upToDate'
        List<StockTransaction> txs = stockTransactionRepository
                .findByLocationIdAndItemIdAndDateLessThanEqual(locationId, itemId, upToDate);
        // sum up the quantityChange
        return txs.stream()
                .mapToDouble(StockTransaction::getQuantityChange)
                .sum();
    }

    private StockTransaction recordTransaction(Location location,
                                               InventoryItem item,
                                               Double qtyChange,
                                               Double costChange,
                                               String transactionType,
                                               Long sourceReferenceId,
                                               LocalDate date) {
        StockTransaction tx = new StockTransaction();
        tx.setLocation(location);
        tx.setItem(item);
        tx.setQuantityChange(qtyChange);
        tx.setCostChange(costChange);
        tx.setTransactionType(transactionType);
        tx.setSourceReferenceId(sourceReferenceId);
        tx.setDate(date != null ? date : LocalDate.now());

        return stockTransactionRepository.save(tx);
    }

    @Override
    public void deleteBySourceReferenceId(Long referenceId) {
        List<StockTransaction> txList = stockTransactionRepository.findBySourceReferenceId(referenceId);
        stockTransactionRepository.deleteAll(txList);
    }

}
