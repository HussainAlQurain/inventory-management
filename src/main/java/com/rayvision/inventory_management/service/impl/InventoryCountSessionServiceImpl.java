package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.repository.*;
import com.rayvision.inventory_management.service.InventoryCountSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@Transactional
public class InventoryCountSessionServiceImpl implements InventoryCountSessionService {

    private final InventoryCountSessionRepository sessionRepository;
    private final InventoryCountLineRepository lineRepository;
    private final LocationRepository locationRepository;
    private final UnitOfMeasureRepository uomRepository;
    private final StorageAreaRepository storageAreaRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public InventoryCountSessionServiceImpl(
            InventoryCountSessionRepository sessionRepository,
            InventoryCountLineRepository lineRepository,
            LocationRepository locationRepository,
            UnitOfMeasureRepository uomRepository,
            StorageAreaRepository storageAreaRepository,
            InventoryItemRepository inventoryItemRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.lineRepository = lineRepository;
        this.locationRepository = locationRepository;
        this.uomRepository = uomRepository;
        this.storageAreaRepository = storageAreaRepository;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    public InventoryCountSession createSession(Long locationId, InventoryCountSession session) {
        // Ensure location is valid
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found: " + locationId));

        session.setLocation(location);
        session.setLocked(false);
        if (session.getCountDate() == null) {
            session.setCountDate(LocalDate.now()); // default
        }

        // Possibly add lines automatically, or just create an empty session (might add all items automatically in the location)
        // Then the UI can call addLine(...).

        return sessionRepository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryCountSession getSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Count session not found: " + sessionId));
    }

    @Override
    public InventoryCountSession updateSession(Long sessionId, InventoryCountSession session) {
        InventoryCountSession existing = getSession(sessionId);
        if (existing.isLocked()) {
            throw new RuntimeException("Cannot update locked session");
        }
        // Update simple fields
        if (session.getCountDate() != null) {
            existing.setCountDate(session.getCountDate());
        }
        if (session.getDescription() != null) {
            existing.setDescription(session.getDescription());
        }
        if (session.getDayPart() != null) {
            existing.setDayPart(session.getDayPart());
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

    // ------------------------------------------------------------------------
    // LINES
    // ------------------------------------------------------------------------
    @Override
    public InventoryCountLine addLine(Long sessionId, InventoryCountLine line) {
        InventoryCountSession session = getSession(sessionId);
        if (session.isLocked()) {
            throw new RuntimeException("Cannot add lines to locked session");
        }
        // Link
        line.setCountSession(session);

        // Validate item
        InventoryItem item = inventoryItemRepository.findById(line.getInventoryItem().getId())
                .orElseThrow(() -> new RuntimeException("InventoryItem not found"));

        // Validate UOM
        if (line.getCountUom() != null && line.getCountUom().getId() != null) {
            UnitOfMeasure uom = uomRepository.findById(line.getCountUom().getId())
                    .orElseThrow(() -> new RuntimeException("UOM not found"));
            line.setCountUom(uom);
        } else {
            // fallback to item’s inventoryUom
            line.setCountUom(item.getInventoryUom());
        }

        // Validate storage area (optional)
        if (line.getStorageArea() != null && line.getStorageArea().getId() != null) {
            StorageArea sa = storageAreaRepository.findById(line.getStorageArea().getId())
                    .orElseThrow(() -> new RuntimeException("StorageArea not found"));
            line.setStorageArea(sa);
        }

        // Convert to base UOM if you want
        Double conversionFactor =
                (line.getCountUom() != null && line.getCountUom().getConversionFactor() != null)
                        ? line.getCountUom().getConversionFactor()
                        : 1.0;
        double baseQty =
                Optional.ofNullable(line.getCountedQuantity()).orElse(0.0) * conversionFactor;
        line.setConvertedQuantityInBaseUom(baseQty);

        // Compute line value if you want
        Double currentPrice = Optional.ofNullable(item.getCurrentPrice()).orElse(0.0);
        line.setLineTotalValue(baseQty * currentPrice);

        return lineRepository.save(line);
    }

    @Override
    public InventoryCountLine updateLine(Long sessionId, Long lineId, InventoryCountLine patchLine) {
        InventoryCountSession session = getSession(sessionId);
        if (session.isLocked()) {
            throw new RuntimeException("Cannot update lines of a locked session");
        }

        // Find the line
        InventoryCountLine existing = lineRepository.findById(lineId)
                .orElseThrow(() -> new RuntimeException("Line not found"));

        // Ensure this line belongs to the same session
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

        // Recompute conversion + line value
        Double conversionFactor =
                (existing.getCountUom() != null && existing.getCountUom().getConversionFactor() != null)
                        ? existing.getCountUom().getConversionFactor()
                        : 1.0;
        double baseQty = Optional.ofNullable(existing.getCountedQuantity()).orElse(0.0) * conversionFactor;
        existing.setConvertedQuantityInBaseUom(baseQty);

        Double currentPrice = Optional.ofNullable(existing.getInventoryItem().getCurrentPrice()).orElse(0.0);
        existing.setLineTotalValue(baseQty * currentPrice);

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
