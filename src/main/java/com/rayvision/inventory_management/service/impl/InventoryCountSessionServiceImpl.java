package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.mappers.InventoryCountSessionMapper;
import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.InventoryCountSessionDTO;
import com.rayvision.inventory_management.repository.*;
import com.rayvision.inventory_management.service.InventoryCountSessionService;
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
    private final InventoryCountSessionMapper sessionMapper;
    private final AssortmentLocationRepository assortmentLocationRepository;

    public InventoryCountSessionServiceImpl(
            InventoryCountSessionRepository sessionRepository,
            InventoryCountLineRepository lineRepository,
            LocationRepository locationRepository,
            UnitOfMeasureRepository uomRepository,
            StorageAreaRepository storageAreaRepository,
            InventoryItemRepository inventoryItemRepository,
            InventoryCountSessionMapper sessionMapper,
            AssortmentLocationRepository assortmentLocationRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.lineRepository = lineRepository;
        this.locationRepository = locationRepository;
        this.uomRepository = uomRepository;
        this.storageAreaRepository = storageAreaRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.sessionMapper = sessionMapper;
        this.assortmentLocationRepository = assortmentLocationRepository;
    }

    // --------------------------------------------------------------------------
    // CREATE SESSION with lines for all items
    // --------------------------------------------------------------------------
    @Override
    public InventoryCountSession createSession(Long locationId, InventoryCountSessionDTO dto) {
        // 1) Validate location
        var location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found: " + locationId));

        // 2) Convert top-level fields from DTO -> entity
        InventoryCountSession sessionEntity = sessionMapper.toEntity(dto);
        sessionEntity.setLocation(location);
        sessionEntity.setLocked(false);

        if (sessionEntity.getCountDate() == null) {
            sessionEntity.setCountDate(LocalDate.now());
        }

        // 3) Build a map of lines from the DTO (if any were passed in).
        Map<Long, InventoryCountLine> linesFromDto = new HashMap<>();
        if (dto.getLines() != null) {
            for (var lineDto : dto.getLines()) {
                InventoryCountLine lineEntity = sessionMapper.toLineEntity(lineDto);

                // fetch item
                InventoryItem item = inventoryItemRepository.findById(lineDto.getInventoryItemId())
                        .orElseThrow(() -> new RuntimeException("InventoryItem not found: " + lineDto.getInventoryItemId()));
                lineEntity.setInventoryItem(item);

                // fetch optional storageArea
                if (lineDto.getStorageAreaId() != null) {
                    StorageArea sa = storageAreaRepository.findById(lineDto.getStorageAreaId())
                            .orElseThrow(() -> new RuntimeException("StorageArea not found: " + lineDto.getStorageAreaId()));
                    lineEntity.setStorageArea(sa);
                }
                // fetch optional countUom
                if (lineDto.getCountUomId() != null) {
                    UnitOfMeasure uom = uomRepository.findById(lineDto.getCountUomId())
                            .orElseThrow(() -> new RuntimeException("UOM not found: " + lineDto.getCountUomId()));
                    lineEntity.setCountUom(uom);
                } else {
                    lineEntity.setCountUom(item.getInventoryUom());
                }

                // compute conversions
                Double qty = Optional.ofNullable(lineDto.getCountedQuantity()).orElse(0.0);
                double convFactor = Optional.ofNullable(lineEntity.getCountUom())
                        .map(UnitOfMeasure::getConversionFactor)
                        .orElse(1.0);
                double baseQty = qty * convFactor;
                lineEntity.setConvertedQuantityInBaseUom(baseQty);

                double price = Optional.ofNullable(item.getCurrentPrice()).orElse(0.0);
                lineEntity.setLineTotalValue(baseQty * price);

                // Link to session
                lineEntity.setCountSession(sessionEntity);

                // put in map by itemId
                linesFromDto.put(item.getId(), lineEntity);
            }
        }

        // 4) Now fetch ALL items for this location’s company
        //    (or if you have a custom relationship, only items that "belong" to that location).
        Long companyId = location.getCompany().getId();
        // (A) find bridging records
        List<AssortmentLocation> bridgingList = assortmentLocationRepository.findByLocationId(locationId);
        // (B) gather items in a set
        Set<InventoryItem> unionItems = new HashSet<>();
        if (bridgingList.isEmpty()) {
            // Fallback
            List<InventoryItem> allCompanyItems = inventoryItemRepository.findByCompanyId(companyId);
            unionItems.addAll(allCompanyItems);
        } else {
            // Normal logic: gather items from all assigned assortments
            for (AssortmentLocation al : bridgingList) {
                Assortment asst = al.getAssortment();
                if (asst.getInventoryItems() != null) {
                    unionItems.addAll(asst.getInventoryItems());
                }
            }
        }



        // 5) For each item, if we don’t already have a line from the DTO, create a new line with 0.0
        for (InventoryItem item : unionItems) {
            if (!linesFromDto.containsKey(item.getId())) {
                // Create an empty line for that item
                InventoryCountLine newLine = new InventoryCountLine();
                newLine.setInventoryItem(item);
                newLine.setCountedQuantity(0.0);
                newLine.setCountUom(item.getInventoryUom());
                newLine.setConvertedQuantityInBaseUom(0.0);
                newLine.setLineTotalValue(0.0);
                newLine.setCountSession(sessionEntity);

                // (No storageArea by default, or you can guess one)
                // e.g., newLine.setStorageArea(null);

                // add to linesFromDto so we eventually add to session
                linesFromDto.put(item.getId(), newLine);
            }
        }

        // 6) Attach all lines to the session
        sessionEntity.getLines().addAll(linesFromDto.values());

        // 7) Save
        return sessionRepository.save(sessionEntity);
    }

    // --------------------------------------------------------------------------
    // READ
    // --------------------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public InventoryCountSession getSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Count session not found: " + sessionId));
    }

    // --------------------------------------------------------------------------
    // UPDATE
    // --------------------------------------------------------------------------
    @Override
    public InventoryCountSession updateSession(Long sessionId, InventoryCountSession patch) {
        InventoryCountSession existing = getSession(sessionId);
        if (existing.isLocked()) {
            throw new RuntimeException("Cannot update locked session");
        }
        // Update simple fields
        if (patch.getCountDate() != null) {
            existing.setCountDate(patch.getCountDate());
        }
        if (patch.getDescription() != null) {
            existing.setDescription(patch.getDescription());
        }
        if (patch.getDayPart() != null) {
            existing.setDayPart(patch.getDayPart());
        }
        // ignoring lines for "updateSession" in this example
        return sessionRepository.save(existing);
    }

    // --------------------------------------------------------------------------
    // DELETE
    // --------------------------------------------------------------------------
    @Override
    public void deleteSession(Long sessionId) {
        InventoryCountSession existing = getSession(sessionId);
        if (existing.isLocked()) {
            throw new RuntimeException("Cannot delete a locked session");
        }
        sessionRepository.delete(existing);
    }

    // --------------------------------------------------------------------------
    // LOCK/UNLOCK
    // --------------------------------------------------------------------------
    @Override
    public InventoryCountSession lockSession(Long sessionId) {
        InventoryCountSession existing = getSession(sessionId);
        existing.setLocked(true);
        existing.setLockedDate(LocalDate.now());
        return sessionRepository.save(existing);
    }

    @Override
    public InventoryCountSession unlockSession(Long sessionId) {
        InventoryCountSession existing = getSession(sessionId);
        existing.setLocked(false);
        existing.setLockedDate(null);
        return sessionRepository.save(existing);
    }

    // --------------------------------------------------------------------------
    // LINES
    // --------------------------------------------------------------------------
    @Override
    public InventoryCountLine addLine(Long sessionId, InventoryCountLine line) {
        InventoryCountSession session = getSession(sessionId);
        if (session.isLocked()) {
            throw new RuntimeException("Cannot add lines to locked session");
        }
        line.setCountSession(session);

        // Validate item
        InventoryItem item = inventoryItemRepository.findById(line.getInventoryItem().getId())
                .orElseThrow(() -> new RuntimeException("InventoryItem not found"));

        // Validate or set UOM
        if (line.getCountUom() != null && line.getCountUom().getId() != null) {
            UnitOfMeasure uom = uomRepository.findById(line.getCountUom().getId())
                    .orElseThrow(() -> new RuntimeException("UOM not found"));
            line.setCountUom(uom);
        } else {
            line.setCountUom(item.getInventoryUom());
        }

        // Validate storage area (optional)
        if (line.getStorageArea() != null && line.getStorageArea().getId() != null) {
            StorageArea sa = storageAreaRepository.findById(line.getStorageArea().getId())
                    .orElseThrow(() -> new RuntimeException("StorageArea not found"));
            line.setStorageArea(sa);
        }

        // Recompute base qty
        double convFactor = Optional.ofNullable(line.getCountUom())
                .map(UnitOfMeasure::getConversionFactor)
                .orElse(1.0);
        double baseQty = Optional.ofNullable(line.getCountedQuantity()).orElse(0.0) * convFactor;
        line.setConvertedQuantityInBaseUom(baseQty);

        double price = Optional.ofNullable(item.getCurrentPrice()).orElse(0.0);
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
                .orElseThrow(() -> new RuntimeException("Line not found"));

        if (!existing.getCountSession().getId().equals(sessionId)) {
            throw new RuntimeException("Line does not belong to session " + sessionId);
        }

        // Update fields
        if (patchLine.getCountedQuantity() != null) {
            existing.setCountedQuantity(patchLine.getCountedQuantity());
        }
        if (patchLine.getCountUom() != null && patchLine.getCountUom().getId() != null) {
            UnitOfMeasure newUom = uomRepository.findById(patchLine.getCountUom().getId())
                    .orElseThrow(() -> new RuntimeException("UOM not found"));
            existing.setCountUom(newUom);
        }
        if (patchLine.getStorageArea() != null && patchLine.getStorageArea().getId() != null) {
            StorageArea sa = storageAreaRepository.findById(patchLine.getStorageArea().getId())
                    .orElseThrow(() -> new RuntimeException("StorageArea not found"));
            existing.setStorageArea(sa);
        }

        // Recompute base qty
        double convFactor = Optional.ofNullable(existing.getCountUom())
                .map(UnitOfMeasure::getConversionFactor)
                .orElse(1.0);
        double baseQty = Optional.ofNullable(existing.getCountedQuantity()).orElse(0.0) * convFactor;
        existing.setConvertedQuantityInBaseUom(baseQty);

        double price = Optional.ofNullable(existing.getInventoryItem().getCurrentPrice()).orElse(0.0);
        existing.setLineTotalValue(baseQty * price);

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

}
