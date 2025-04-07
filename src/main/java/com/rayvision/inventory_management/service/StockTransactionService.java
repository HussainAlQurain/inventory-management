package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.StockLevelDTO;

import java.time.LocalDate;
import java.util.List;

public interface StockTransactionService {

    // -------------------------------------------------------------------------
    // ITEM-based
    // -------------------------------------------------------------------------
    StockTransaction recordPurchase(Location location,
                                    InventoryItem item,
                                    Double countQty,
                                    UnitOfMeasure countUom,
                                    Long sourceReferenceId,
                                    LocalDate date);

    StockTransaction recordUsage(Location location,
                                 InventoryItem item,
                                 Double countQty,
                                 UnitOfMeasure countUom,
                                 Long sourceReferenceId,
                                 LocalDate date);

    StockTransaction recordTransferIn(Location location,
                                      InventoryItem item,
                                      Double countQty,
                                      UnitOfMeasure countUom,
                                      Long sourceReferenceId,
                                      LocalDate date);

    StockTransaction recordTransferOut(Location location,
                                       InventoryItem item,
                                       Double countQty,
                                       UnitOfMeasure countUom,
                                       Long sourceReferenceId,
                                       LocalDate date);

    StockTransaction recordAdjustment(Location location,
                                      InventoryItem item,
                                      Double countQty,
                                      UnitOfMeasure countUom,
                                      Long sourceReferenceId,
                                      LocalDate date);


    // -------------------------------------------------------------------------
    // SUBRECIPE-based
    // -------------------------------------------------------------------------
    StockTransaction recordPurchase(Location location,
                                    SubRecipe sub,
                                    Double countQty,
                                    UnitOfMeasure countUom,
                                    Long sourceReferenceId,
                                    LocalDate date);

    StockTransaction recordUsage(Location location,
                                 SubRecipe sub,
                                 Double countQty,
                                 UnitOfMeasure countUom,
                                 Long sourceReferenceId,
                                 LocalDate date);

    StockTransaction recordTransferIn(Location location,
                                      SubRecipe sub,
                                      Double countQty,
                                      UnitOfMeasure countUom,
                                      Long sourceReferenceId,
                                      LocalDate date);

    StockTransaction recordTransferOut(Location location,
                                       SubRecipe sub,
                                       Double countQty,
                                       UnitOfMeasure countUom,
                                       Long sourceReferenceId,
                                       LocalDate date);

    StockTransaction recordAdjustment(Location location,
                                      SubRecipe sub,
                                      Double countQty,
                                      UnitOfMeasure countUom,
                                      Long sourceReferenceId,
                                      LocalDate date);

    // -------------------------------------------------------------------------
    // OTHER LEDGER QUERIES
    // -------------------------------------------------------------------------
    double calculateTheoreticalOnHand(Long locationId, Long itemId, LocalDate upToDate);

    double calculateTheoreticalOnHandUnified(Long locationId, Long itemId, Long subRecipeId, LocalDate upToDate);

    void deleteBySourceReferenceId(Long referenceId);

    List<StockLevelDTO> getStockLevelsForLocation(Long locationId, LocalDate startDate, LocalDate endDate);

}