package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.StockTransaction;
import com.rayvision.inventory_management.model.SubRecipe;
import com.rayvision.inventory_management.model.dto.StockLevelDTO;

import java.time.LocalDate;
import java.util.List;

public interface StockTransactionService {

    // --------------------
    // ITEM-based methods
    // --------------------
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

    // --------------------
    // SUBRECIPE-based methods
    // --------------------
    StockTransaction recordPurchase(Location location,
                                    SubRecipe sub,
                                    Double qty,
                                    Double cost,
                                    Long sourceReferenceId,
                                    LocalDate date);

    StockTransaction recordTransferIn(Location location,
                                      SubRecipe sub,
                                      Double qty,
                                      Double cost,
                                      Long sourceReferenceId,
                                      LocalDate date);

    StockTransaction recordTransferOut(Location location,
                                       SubRecipe sub,
                                       Double qty,
                                       Double cost,
                                       Long sourceReferenceId,
                                       LocalDate date);

    StockTransaction recordUsage(Location location,
                                 SubRecipe sub,
                                 Double qty,
                                 Double cost,
                                 Long sourceReferenceId,
                                 LocalDate date);

    StockTransaction recordAdjustment(Location location,
                                      SubRecipe sub,
                                      Double qty,
                                      Double cost,
                                      Long sourceReferenceId,
                                      LocalDate date);

    // --------------------
    // LEDGER QUERIES
    // --------------------
    void deleteBySourceReferenceId(Long referenceId);

    /**
     * For item-based back-compat
     */
    double calculateTheoreticalOnHand(Long locationId, Long itemId, LocalDate upToDate);

    /**
     * Unified approach: either itemId or subRecipeId (not both).
     */
    double calculateTheoreticalOnHandUnified(Long locationId,
                                             Long itemId,
                                             Long subRecipeId,
                                             LocalDate upToDate);

    List<StockLevelDTO> getStockLevelsForLocation(Long locationId,
                                                         LocalDate startDate,
                                                         LocalDate endDate);

}