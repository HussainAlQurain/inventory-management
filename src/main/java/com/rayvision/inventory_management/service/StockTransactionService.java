package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.StockTransaction;

import java.time.LocalDate;

public interface StockTransactionService {
    StockTransaction recordPurchase(Location location,
                                    InventoryItem item,
                                    Double qty,
                                    Double cost,
                                    Long sourceReferenceId,
                                    LocalDate date);

    StockTransaction recordTransferIn(Location location,
                                      InventoryItem item,
                                      Double qty,
                                      Double cost,
                                      Long sourceReferenceId,
                                      LocalDate date);

    StockTransaction recordTransferOut(Location location,
                                       InventoryItem item,
                                       Double qty,
                                       Double cost,
                                       Long sourceReferenceId,
                                       LocalDate date);

    StockTransaction recordUsage(Location location,
                                 InventoryItem item,
                                 Double qty,
                                 Double cost,
                                 Long sourceReferenceId,
                                 LocalDate date);

    StockTransaction recordAdjustment(Location location,
                                      InventoryItem item,
                                      Double qty,
                                      Double cost,
                                      Long sourceReferenceId,
                                      LocalDate date);

    void deleteBySourceReferenceId(Long referenceId);
    double calculateTheoreticalOnHand(Long locationId, Long itemId, LocalDate upToDate);

}
