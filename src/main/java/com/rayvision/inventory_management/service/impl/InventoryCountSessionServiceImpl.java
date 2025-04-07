package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.enums.SubRecipeType;
import com.rayvision.inventory_management.mappers.InventoryCountSessionMapper;
import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.*;
import com.rayvision.inventory_management.repository.*;
import com.rayvision.inventory_management.service.InventoryCountSessionService;
import com.rayvision.inventory_management.service.StockTransactionService;
import com.rayvision.inventory_management.service.InventoryItemLocationService;
import com.rayvision.inventory_management.service.PrepItemLocationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class InventoryCountSessionServiceImpl implements InventoryCountSessionService {

    private final InventoryCountSessionRepository sessionRepository;
    private final InventoryCountLineRepository lineRepository;
    private final LocationRepository locationRepository;
    private final UnitOfMeasureRepository uomRepository;
    private final StorageAreaRepository storageAreaRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SubRecipeRepository subRecipeRepository; // NEW
    private final InventoryCountSessionMapper inventoryCountSessionMapper;
    private final AssortmentLocationRepository assortmentLocationRepository;
    private final StockTransactionService stockTransactionService;

    // bridging services to update onHand after lock
    private final InventoryItemLocationService inventoryItemLocationService;
    private final PrepItemLocationService prepItemLocationService;
    private final CountUomPreferenceRepository countUomPreferenceRepository;

    public InventoryCountSessionServiceImpl(
            InventoryCountSessionRepository sessionRepository,
            InventoryCountLineRepository lineRepository,
            LocationRepository locationRepository,
            UnitOfMeasureRepository uomRepository,
            StorageAreaRepository storageAreaRepository,
            InventoryItemRepository inventoryItemRepository,
            SubRecipeRepository subRecipeRepository,       // NEW
            InventoryCountSessionMapper inventoryCountSessionMapper,
            AssortmentLocationRepository assortmentLocationRepository,
            StockTransactionService stockTransactionService,
            InventoryItemLocationService inventoryItemLocationService,
            PrepItemLocationService prepItemLocationService,
            CountUomPreferenceRepository countUomPreferenceRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.lineRepository = lineRepository;
        this.locationRepository = locationRepository;
        this.uomRepository = uomRepository;
        this.storageAreaRepository = storageAreaRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.subRecipeRepository = subRecipeRepository;   // NEW
        this.inventoryCountSessionMapper = inventoryCountSessionMapper;
        this.assortmentLocationRepository = assortmentLocationRepository;
        this.stockTransactionService = stockTransactionService;
        this.inventoryItemLocationService = inventoryItemLocationService;
        this.prepItemLocationService = prepItemLocationService;
        this.countUomPreferenceRepository = countUomPreferenceRepository;
    }

    @Override
    public InventoryCountSession createSession(Long locationId, InventoryCountSessionDTO dto) {
        // 0) Remove logic preventing multiple open sessions (as you already did)

        // 1) Validate location
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found: " + locationId));

        // 2) Convert top-level fields from DTO
        InventoryCountSession sessionEntity = inventoryCountSessionMapper.toEntity(dto);
        sessionEntity.setLocation(location);
        sessionEntity.setLocked(false);
        if (sessionEntity.getCountDate() == null) {
            sessionEntity.setCountDate(LocalDate.now());
        }

        // 3) Convert lines from the DTO
        //    We store them in a map to avoid duplicates: "item-123-uom-456" or "sub-789-uom-456"
        Map<String, InventoryCountLine> linesMap = new HashMap<>();

        if (dto.getLines() != null) {
            for (InventoryCountLineDTO lineDto : dto.getLines()) {
                InventoryCountLine lineEntity = inventoryCountSessionMapper.toLineEntity(lineDto);

                // EITHER item or subRecipe
                if (lineDto.getInventoryItemId() != null) {
                    InventoryItem item = inventoryItemRepository
                            .findById(lineDto.getInventoryItemId())
                            .orElseThrow(() -> new RuntimeException(
                                    "Item not found: " + lineDto.getInventoryItemId()));
                    lineEntity.setInventoryItem(item);

                } else if (lineDto.getSubRecipeId() != null) {
                    SubRecipe sub = subRecipeRepository
                            .findById(lineDto.getSubRecipeId())
                            .orElseThrow(() -> new RuntimeException(
                                    "SubRecipe not found: " + lineDto.getSubRecipeId()));
                    lineEntity.setSubRecipe(sub);
                }

                // optional storageArea
                if (lineDto.getStorageAreaId() != null) {
                    StorageArea sa = storageAreaRepository.findById(lineDto.getStorageAreaId())
                            .orElseThrow(() -> new RuntimeException(
                                    "StorageArea not found: " + lineDto.getStorageAreaId()));
                    lineEntity.setStorageArea(sa);
                }

                // countUom
                if (lineDto.getCountUomId() != null) {
                    UnitOfMeasure uom = uomRepository
                            .findById(lineDto.getCountUomId())
                            .orElseThrow(() -> new RuntimeException(
                                    "UOM not found: " + lineDto.getCountUomId()));
                    lineEntity.setCountUom(uom);
                }

                // =========== NEW LOGIC FOR COST & BASE QTY ===========
                double countedQty = Optional.ofNullable(lineDto.getCountedQuantity()).orElse(0.0);

                if (lineEntity.getInventoryItem() != null) {
                    // use ItemCostCalculator
                    double cost = ItemCostCalculator.computeCost(
                            lineEntity.getInventoryItem(),
                            countedQty,
                            lineEntity.getCountUom()
                    );
                    lineEntity.setLineTotalValue(cost);

                    // also store the item’s base quantity
                    double ratio = lineEntity.getCountUom().getConversionFactor()
                            / lineEntity.getInventoryItem().getInventoryUom().getConversionFactor();
                    double baseQty = countedQty * ratio;
                    lineEntity.setConvertedQuantityInBaseUom(baseQty);

                } else if (lineEntity.getSubRecipe() != null) {
                    // ================= SUB-RECIPE LOGIC =================
                    SubRecipe sub = lineEntity.getSubRecipe();
                    UnitOfMeasure subBaseUom = sub.getUom();

                    // Convert from line’s countUom to sub’s base:
                    double ratio =
                            (lineEntity.getCountUom() != null ? lineEntity.getCountUom().getConversionFactor() : 1.0)
                                    /
                                    (subBaseUom != null ? subBaseUom.getConversionFactor() : 1.0);

                    countedQty = Optional.ofNullable(lineEntity.getCountedQuantity()).orElse(0.0);
                    double subBaseQty = countedQty * ratio;

                    // The subRecipe cost is “total cost for entire yieldQty”
                    double totalBatchCost = Optional.ofNullable(sub.getCost()).orElse(0.0);
                    double yieldQty       = Optional.ofNullable(sub.getYieldQty()).orElse(1.0);

                    // cost per 1.0 sub base
                    double costPerBaseUom = (yieldQty != 0.0) ? (totalBatchCost / yieldQty) : 0.0;

// final line cost
                    double cost = subBaseQty * costPerBaseUom;

                    lineEntity.setConvertedQuantityInBaseUom(subBaseQty);
                    lineEntity.setLineTotalValue(cost);

                } else {
                    // no item or sub? zero everything
                    lineEntity.setLineTotalValue(0.0);
                    lineEntity.setConvertedQuantityInBaseUom(0.0);
                }
                // =========== END NEW LOGIC ===========

                lineEntity.setCountSession(sessionEntity);

                // Build the key
                String key = buildLineKey(lineEntity);
                linesMap.put(key, lineEntity);
            }
        }

        // 4) Find union of Items & (optionally) SubRecipes from the location’s assortments
        Long companyId = location.getCompany().getId();
        List<AssortmentLocation> bridgingList = assortmentLocationRepository.findByLocationId(locationId);

        // union of InventoryItems
        Set<InventoryItem> unionItems = new HashSet<>();
        if (bridgingList.isEmpty()) {
            // fallback to all items in the company
            unionItems.addAll(inventoryItemRepository.findByCompanyId(companyId));
        } else {
            for (AssortmentLocation al : bridgingList) {
                unionItems.addAll(al.getAssortment().getInventoryItems());
            }
        }

        // union of SubRecipes (only if you want them in the same count session)
        Set<SubRecipe> unionPreps = new HashSet<>();
        if (bridgingList.isEmpty()) {
            unionPreps.addAll(subRecipeRepository.findByCompanyIdAndType(
                    companyId, SubRecipeType.PREPARATION));
        } else {
            for (AssortmentLocation al : bridgingList) {
                unionPreps.addAll(al.getAssortment().getSubRecipes());
            }
        }

        // 5) Create lines for each item based on CountUomPreference
        for (InventoryItem item : unionItems) {
            List<CountUomPreference> prefs = countUomPreferenceRepository.findByInventoryItemId(item.getId());
            if (prefs.isEmpty()) {
                // No preferences => create one line with the item’s inventoryUom (if not in map)
                addLineIfNotPresent(linesMap, sessionEntity, item, null, item.getInventoryUom());
            } else {
                for (CountUomPreference pref : prefs) {
                    addLineIfNotPresent(
                            linesMap, sessionEntity,
                            item, null,
                            pref.getCountUom()
                    );
                }
            }
        }

        // 6) Create lines for each subRecipe
        for (SubRecipe prep : unionPreps) {
            List<CountUomPreference> prefs = countUomPreferenceRepository.findBySubRecipeId(prep.getId());
            if (prefs.isEmpty()) {
                addLineIfNotPresent(linesMap, sessionEntity, null, prep, prep.getUom());
            } else {
                for (CountUomPreference pref : prefs) {
                    addLineIfNotPresent(
                            linesMap, sessionEntity,
                            null, prep,
                            pref.getCountUom()
                    );
                }
            }
        }

        // 7) Final: put all lines into session
        sessionEntity.getLines().addAll(linesMap.values());

        // Save
        return sessionRepository.save(sessionEntity);
    }


    /**
     * Helper method to create a line (if it doesn't already exist)
     * Key is "item-123-uom-45" or "sub-678-uom-45"
     */
    private void addLineIfNotPresent(
            Map<String, InventoryCountLine> linesMap,
            InventoryCountSession session,
            InventoryItem item,
            SubRecipe sub,
            UnitOfMeasure uom
    ) {
        // Build the key
        String key;
        if (item != null) {
            key = "item-" + item.getId() + "-uom-" + (uom != null ? uom.getId() : 0);
        } else {
            key = "sub-" + sub.getId() + "-uom-" + (uom != null ? uom.getId() : 0);
        }

        if (!linesMap.containsKey(key)) {
            InventoryCountLine newLine = new InventoryCountLine();
            newLine.setCountSession(session);
            newLine.setInventoryItem(item);
            newLine.setSubRecipe(sub);
            newLine.setCountUom(uom);

            // defaulted to zero until user enters a count
            newLine.setCountedQuantity(0.0);

            // compute base
            double convFactor = (uom != null && uom.getConversionFactor() != null)
                    ? uom.getConversionFactor() : 1.0;
            newLine.setConvertedQuantityInBaseUom(0.0);
            newLine.setLineTotalValue(0.0);  // user will fill

            linesMap.put(key, newLine);
        }
    }

    /** Build a line key from an existing line. */
    private String buildLineKey(InventoryCountLine line) {
        Long itemId = (line.getInventoryItem() != null) ? line.getInventoryItem().getId() : null;
        Long subId  = (line.getSubRecipe() != null) ? line.getSubRecipe().getId() : null;
        Long uomId  = (line.getCountUom() != null)  ? line.getCountUom().getId() : 0;

        if (itemId != null) {
            return "item-" + itemId + "-uom-" + uomId;
        } else {
            return "sub-" + subId + "-uom-" + uomId;
        }
    }


    @Override
    public InventoryCountSession getSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Count session not found: " + sessionId));
    }

    @Override
    public InventoryCountSession updateSession(Long sessionId, InventoryCountSession patch) {
        InventoryCountSession existing = getSession(sessionId);
        if (existing.isLocked()) {
            throw new RuntimeException("Cannot update locked session");
        }
        if (patch.getCountDate() != null) {
            existing.setCountDate(patch.getCountDate());
        }
        if (patch.getDescription() != null) {
            existing.setDescription(patch.getDescription());
        }
        if (patch.getDayPart() != null) {
            existing.setDayPart(patch.getDayPart());
        }
        return sessionRepository.save(existing);
    }

    @Override
    public void deleteSession(Long sessionId) {
        InventoryCountSession existing = getSession(sessionId);
        if (existing.isLocked()) {
            throw new RuntimeException("Cannot delete a locked session");
        }
        sessionRepository.delete(existing);
    }

    @Override
    public InventoryCountSession lockSession(Long sessionId) {
        InventoryCountSession existing = getSession(sessionId);
        if (existing.isLocked()) {
            throw new RuntimeException("Session is already locked");
        }

        // Mark locked
        existing.setLocked(true);
        existing.setLockedDate(LocalDate.now());
        sessionRepository.save(existing);

        // For each line, unify the "theoretical" logic for item or subRecipe
        Location loc = existing.getLocation();
        LocalDate countDate = existing.getCountDate();

        for (InventoryCountLine line : existing.getLines()) {
            double actual = Optional.ofNullable(line.getConvertedQuantityInBaseUom()).orElse(0.0);

            InventoryItem item = line.getInventoryItem();
            SubRecipe sub = line.getSubRecipe();

            // Use the unified approach if you want subRecipes in your ledger:
            Long itemId = (item != null) ? item.getId() : null;
            Long subId = (sub != null) ? sub.getId() : null;

            // Theoretical from the ledger (either item or subRecipe) using the new method
            double theoretical = stockTransactionService
                    .calculateTheoreticalOnHandUnified(loc.getId(), itemId, subId, countDate);

            double diff = actual - theoretical;

            // 1) Post ADJUSTMENT if the difference is significant
            if (Math.abs(diff) > 0.000001) {
                if (item != null) {
                    // old code had “diff * unitCost” as cost, but now you pass `countQty=diff`
                    // and `countUom=item.getInventoryUom()`
                    stockTransactionService.recordAdjustment(
                            loc,
                            item,
                            diff,
                            item.getInventoryUom(),   // or line.getCountUom() if you prefer
                            existing.getId(),
                            countDate
                    );
                } else if (sub != null) {
                    // sub’s base
                    stockTransactionService.recordAdjustment(
                            loc,
                            sub,
                            diff,
                            sub.getUom(),
                            existing.getId(),
                            countDate
                    );
                }
            }

            // 2) Update bridging for onHand/lastCount
            if (item != null) {
                inventoryItemLocationService.createOrUpdateByItemAndLocation(
                        InventoryItemLocationDTO.builder()
                                .inventoryItemId(item.getId())
                                .locationId(loc.getId())
                                .onHand(actual)
                                .lastCount(actual)
                                .lastCountDate(countDate)
                                .build()
                );
            } else if (sub != null) {
                prepItemLocationService.createOrUpdate(
                        PrepItemLocationDTO.builder()
                                .subRecipeId(sub.getId())
                                .locationId(loc.getId())
                                .onHand(actual)
                                .lastCount(actual)
                                .lastCountDate(countDate)
                                .build()
                );
            }
        }

        return existing;
    }

    @Override
    public InventoryCountSession unlockSession(Long sessionId) {
        InventoryCountSession existing = getSession(sessionId);
        if (!existing.isLocked()) {
            throw new RuntimeException("Session is not locked");
        }
        // remove ADJUSTMENT transactions
        stockTransactionService.deleteBySourceReferenceId(sessionId);

        existing.setLocked(false);
        existing.setLockedDate(null);
        return sessionRepository.save(existing);
    }

    // LINES
    @Override
    public InventoryCountLine addLine(Long sessionId, InventoryCountLine line) {
        InventoryCountSession session = getSession(sessionId);
        if (session.isLocked()) {
            throw new RuntimeException("Cannot add lines to locked session");
        }
        line.setCountSession(session);

        // if item or subRecipe?
        if (line.getInventoryItem() != null && line.getInventoryItem().getId() != null) {
            InventoryItem item = inventoryItemRepository.findById(line.getInventoryItem().getId())
                    .orElseThrow(() -> new RuntimeException("InventoryItem not found"));
            line.setInventoryItem(item);
            // set the default countUom if needed
            if (line.getCountUom() == null) {
                line.setCountUom(item.getInventoryUom());
            }
        } else if (line.getSubRecipe() != null && line.getSubRecipe().getId() != null) {
            SubRecipe sub = subRecipeRepository.findById(line.getSubRecipe().getId())
                    .orElseThrow(() -> new RuntimeException("SubRecipe not found"));
            line.setSubRecipe(sub);
        }

        if (line.getCountUom() != null && line.getCountUom().getId() != null) {
            UnitOfMeasure uom = uomRepository.findById(line.getCountUom().getId())
                    .orElseThrow(() -> new RuntimeException("UOM not found"));
            line.setCountUom(uom);
        }

        // storage area
        if (line.getStorageArea() != null && line.getStorageArea().getId() != null) {
            StorageArea sa = storageAreaRepository.findById(line.getStorageArea().getId())
                    .orElseThrow(() -> new RuntimeException("StorageArea not found"));
            line.setStorageArea(sa);
        }

        double convFactor = Optional.ofNullable(line.getCountUom())
                .map(UnitOfMeasure::getConversionFactor).orElse(1.0);
        double baseQty = Optional.ofNullable(line.getCountedQuantity()).orElse(0.0) * convFactor;
        line.setConvertedQuantityInBaseUom(baseQty);

        double price = 0.0;
        if (line.getInventoryItem() != null) {
            price = Optional.ofNullable(line.getInventoryItem().getCurrentPrice()).orElse(0.0);
        }
        line.setLineTotalValue(baseQty * price);

        return lineRepository.save(line);
    }

    @Override
    public InventoryCountLine updateLine(Long sessionId, Long lineId, InventoryCountLine patchLine) {
        InventoryCountSession session = getSession(sessionId);
        if (session.isLocked()) {
            throw new RuntimeException("Cannot update lines of a locked session");
        }

        InventoryCountLine existing = lineRepository.findById(lineId)
                .orElseThrow(() -> new RuntimeException("Line not found: " + lineId));

        if (!existing.getCountSession().getId().equals(sessionId)) {
            throw new RuntimeException("Line does not belong to session " + sessionId);
        }

        // Update counted quantity
        if (patchLine.getCountedQuantity() != null) {
            existing.setCountedQuantity(patchLine.getCountedQuantity());
        }

        // Update the countUom if it changed
        if (patchLine.getCountUom() != null && patchLine.getCountUom().getId() != null) {
            UnitOfMeasure newUom = uomRepository.findById(patchLine.getCountUom().getId())
                    .orElseThrow(() -> new RuntimeException("UOM not found: " + patchLine.getCountUom().getId()));
            existing.setCountUom(newUom);
        }

        // Update storage area if needed
        if (patchLine.getStorageArea() != null && patchLine.getStorageArea().getId() != null) {
            StorageArea sa = storageAreaRepository.findById(patchLine.getStorageArea().getId())
                    .orElseThrow(() -> new RuntimeException("StorageArea not found: " + patchLine.getStorageArea().getId()));
            existing.setStorageArea(sa);
        }

        // Recompute cost & convertedQuantityInBaseUom
        double countedQty = Optional.ofNullable(existing.getCountedQuantity()).orElse(0.0);

        if (existing.getInventoryItem() != null) {
            // -- ITEM LOGIC (already done) --
            double cost = ItemCostCalculator.computeCost(
                    existing.getInventoryItem(),
                    countedQty,
                    existing.getCountUom()
            );
            existing.setLineTotalValue(cost);

            // Store the item’s base quantity if you want
            double ratio = existing.getCountUom().getConversionFactor()
                    / existing.getInventoryItem().getInventoryUom().getConversionFactor();
            double baseQty = countedQty * ratio;
            existing.setConvertedQuantityInBaseUom(baseQty);

        } else if (existing.getSubRecipe() != null) {
            // SUB-RECIPE LOGIC
            SubRecipe sub = existing.getSubRecipe();
            UnitOfMeasure subBaseUom = sub.getUom();

            // Convert from line’s countUom to sub’s base:
            double ratio =
                    (existing.getCountUom() != null ? existing.getCountUom().getConversionFactor() : 1.0)
                            /
                            (subBaseUom != null ? subBaseUom.getConversionFactor() : 1.0);

            double subBaseQty = countedQty * ratio;   // countedQty is already declared above
            double totalBatchCost = Optional.ofNullable(sub.getCost()).orElse(0.0);
            double yieldQty       = Optional.ofNullable(sub.getYieldQty()).orElse(1.0);

            // cost per 1 sub base UOM
            double costPerBaseUom = (yieldQty != 0.0) ? (totalBatchCost / yieldQty) : 0.0;
            double cost = subBaseQty * costPerBaseUom;

            existing.setConvertedQuantityInBaseUom(subBaseQty);
            existing.setLineTotalValue(cost);

        } else {
            // neither item nor sub => zero cost
            existing.setLineTotalValue(0.0);
            existing.setConvertedQuantityInBaseUom(0.0);
        }

        return lineRepository.save(existing);
    }



    @Override
    public void deleteLine(Long sessionId, Long lineId) {
        InventoryCountSession session = getSession(sessionId);
        if (session.isLocked()) {
            throw new RuntimeException("Cannot delete lines from locked session");
        }

        InventoryCountLine existing = lineRepository.findById(lineId)
                .orElseThrow(() -> new RuntimeException("Line not found"));
        if (!existing.getCountSession().getId().equals(sessionId)) {
            throw new RuntimeException("Line does not belong to session " + sessionId);
        }
        lineRepository.delete(existing);
    }

    @Override
    public List<InventoryCountSessionSummaryDTO> findByCompanyAndDateRange(Long companyId,
                                                                 LocalDate startDate,
                                                                 LocalDate endDate) {
        // If null checks are needed:
        if (startDate == null) startDate = LocalDate.of(1970,1,1); // or some default
        if (endDate == null)   endDate   = LocalDate.now();

        // 1) fetch
        List<InventoryCountSession> sessions =
                sessionRepository.findByCompanyAndDateRange(companyId, startDate, endDate);

        // 2) convert
        return sessions.stream()
                .map(this::toSummaryDTO)   // call the private method
                .toList();

    }

    public double computeSessionValue(InventoryCountSession session) {
        if (session.getLines() == null) return 0.0;
        return session.getLines().stream()
                .mapToDouble(line -> Optional.ofNullable(line.getLineTotalValue()).orElse(0.0))
                .sum();
    }

    private InventoryCountSessionSummaryDTO toSummaryDTO(InventoryCountSession session) {
        InventoryCountSessionSummaryDTO dto = new InventoryCountSessionSummaryDTO();
        dto.setId(session.getId());
        dto.setCountDate(session.getCountDate());
        dto.setDayPart(session.getDayPart());
        dto.setDescription(session.getDescription());

        if (session.getLocation() != null) {
            dto.setLocationName(session.getLocation().getName());
        }

        // sum of lineTotalValue
        double totalValue = computeSessionValue(session);
        dto.setValueOfCount(totalValue);

        dto.setLocked(session.isLocked());

        return dto;
    }

}
