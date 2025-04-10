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
                                    Double qty,
                                    UnitOfMeasure uom,
                                    Long sourceReferenceId,
                                    LocalDate date);
    /**
     * Overloaded method: same as above, but you can specify
     * overridePrice for cost calculations, and
     * updateOptionPrice if you want to adopt that price in the PurchaseOption.
     */
    StockTransaction recordPurchase(Location location,
                                    InventoryItem item,
                                    Double qty,
                                    UnitOfMeasure uom,
                                    Long sourceReferenceId,
                                    LocalDate date,
                                    Double overridePrice,
                                    boolean updateOptionPrice);


    StockTransaction recordUsage(Location location,
                                 InventoryItem item,
                                 Double qty,
                                 UnitOfMeasure uom,
                                 Long sourceReferenceId,
                                 LocalDate date);

    StockTransaction recordTransferIn(Location location,
                                      InventoryItem item,
                                      Double qty,
                                      UnitOfMeasure uom,
                                      Long sourceReferenceId,
                                      LocalDate date);

    StockTransaction recordTransferOut(Location location,
                                       InventoryItem item,
                                       Double qty,
                                       UnitOfMeasure uom,
                                       Long sourceReferenceId,
                                       LocalDate date);

    StockTransaction recordAdjustment(Location location,
                                      InventoryItem item,
                                      Double qty,
                                      UnitOfMeasure uom,
                                      Long sourceReferenceId,
                                      LocalDate date);


    // -------------------------------------------------------------------------
    // SUBRECIPE-based
    // -------------------------------------------------------------------------
    // ---------------------------------------------------------
    // SUBRECIPE-based (if you want them)
    // ---------------------------------------------------------
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