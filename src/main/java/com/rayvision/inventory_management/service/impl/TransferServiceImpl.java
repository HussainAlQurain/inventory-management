package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.TransferCreateDTO;
import com.rayvision.inventory_management.model.dto.TransferLineDTO;
import com.rayvision.inventory_management.repository.*;
import com.rayvision.inventory_management.service.StockTransactionService;
import com.rayvision.inventory_management.service.TransferService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class TransferServiceImpl implements TransferService {
    private final TransferRepository transferRepository;
    private final LocationRepository locationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SubRecipeRepository subRecipeRepository;
    private final UnitOfMeasureRepository uomRepository;   // so we can fetch the user’s chosen UOM
    private final StockTransactionService stockTransactionService;

    public TransferServiceImpl(TransferRepository transferRepository,
                               LocationRepository locationRepository,
                               InventoryItemRepository inventoryItemRepository,
                               StockTransactionService stockTransactionService,
                               UnitOfMeasureRepository uomRepository,
                               SubRecipeRepository subRecipeRepository) {
        this.transferRepository = transferRepository;
        this.locationRepository = locationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockTransactionService = stockTransactionService;
        this.uomRepository = uomRepository;
        this.subRecipeRepository = subRecipeRepository;
    }

    // ------------------------------------------------------------------ create
    @Override
    public Transfer createTransfer(TransferCreateDTO dto) {

        Location from = locationRepository.findById(dto.getFromLocationId())
                .orElseThrow(() -> new RuntimeException("from‑location not found"));
        Location to   = locationRepository.findById(dto.getToLocationId())
                .orElseThrow(() -> new RuntimeException("to‑location not found"));

        Transfer t = Transfer.builder()
                .creationDate(LocalDate.now())
                .status("DRAFT")
                .fromLocation(from)
                .toLocation(to)
                .build();

        List<TransferLine> lines = new ArrayList<>();
        for (TransferLineDTO l : dto.getLines()) {

            UnitOfMeasure uom = uomRepository.findById(l.getUnitOfMeasureId())
                    .orElseThrow(() -> new RuntimeException("UoM not found: " + l.getUnitOfMeasureId()));

            TransferLine entity = new TransferLine();
            entity.setTransfer(t);
            entity.setUnitOfMeasure(uom);
            entity.setQuantity(l.getQuantity());

            /* item OR subRecipe */
            if (l.getInventoryItemId() != null) {
                InventoryItem item = inventoryItemRepository.findById(l.getInventoryItemId())
                        .orElseThrow(() -> new RuntimeException("Item not found: " + l.getInventoryItemId()));
                entity.setInventoryItem(item);
            } else if (l.getSubRecipeId() != null) {
                SubRecipe sub = subRecipeRepository.findById(l.getSubRecipeId())
                        .orElseThrow(() -> new RuntimeException("SubRecipe not found: " + l.getSubRecipeId()));
                entity.setSubRecipe(sub);
            } else {
                throw new RuntimeException("Line must have inventoryItemId or subRecipeId");
            }

            /* optional cost fields */
            entity.setCostPerUnit(l.getCostPerUnit());
            if (l.getCostPerUnit() != null && l.getQuantity() != null) {
                entity.setTotalCost(l.getCostPerUnit() * l.getQuantity());
            }

            lines.add(entity);
        }
        t.setLines(lines);

        return transferRepository.save(t);
    }

    // TransferServiceImpl  – add after createTransfer()

    @Override
    public List<Transfer> findOutgoingDraftsByLocation(Long locId) {
        return transferRepository.findByFromLocationIdAndStatus(locId, "DRAFT");
    }

    @Override
    public List<Transfer> findIncomingDraftsByLocation(Long locId) {
        return transferRepository.findByToLocationIdAndStatus(locId, "DRAFT");
    }

    @Override
    public List<Transfer> findOutgoingDraftsByCompany(Long companyId) {
        return transferRepository.findOutgoingDraftsByCompany(companyId, "DRAFT");
    }

    @Override
    public List<Transfer> findIncomingDraftsByCompany(Long companyId) {
        return transferRepository.findIncomingDraftsByCompany(companyId, "DRAFT");
    }

    /* ------------------------------------------------ draft update --------- */
    @Override
    public Transfer updateDraft(Long transferId,
                                List<TransferLineDTO> newLines,
                                Long actingLocationId) {

        Transfer t = getTransfer(transferId);

        if (!"DRAFT".equalsIgnoreCase(t.getStatus())) {
            throw new RuntimeException("Only DRAFT transfers can be edited");
        }
        if (!t.getToLocation().getId().equals(actingLocationId)) {
            throw new RuntimeException("Only the receiving location may edit the draft");
        }

        /* 1) wipe current lines (or you can diff‑update if you prefer) */
        t.getLines().clear();

        /* 2) rebuild from DTOs */
        for (TransferLineDTO l : newLines) {

            UnitOfMeasure uom = uomRepository.findById(l.getUnitOfMeasureId())
                    .orElseThrow(() -> new RuntimeException("UoM not found"));

            TransferLine line = new TransferLine();
            line.setTransfer(t);
            line.setUnitOfMeasure(uom);
            line.setQuantity(l.getQuantity());

            if (l.getInventoryItemId() != null) {
                InventoryItem item = inventoryItemRepository.findById(l.getInventoryItemId())
                        .orElseThrow(() -> new RuntimeException("Item not found"));
                line.setInventoryItem(item);
            } else if (l.getSubRecipeId() != null) {
                SubRecipe sub = subRecipeRepository.findById(l.getSubRecipeId())
                        .orElseThrow(() -> new RuntimeException("SubRecipe not found"));
                line.setSubRecipe(sub);
            } else {
                throw new RuntimeException("Line must have inventoryItemId or subRecipeId");
            }

            /* optional costs */
            line.setCostPerUnit(l.getCostPerUnit());
            if (l.getCostPerUnit()!=null && l.getQuantity()!=null) {
                line.setTotalCost(l.getCostPerUnit() * l.getQuantity());
            }

            t.getLines().add(line);
        }

        return transferRepository.save(t);
    }

    // -------------------------------------------------------------- complete
    @Override
    public Transfer completeTransfer(Long transferId) {

        Transfer t = transferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found"));
        if (!"DRAFT".equalsIgnoreCase(t.getStatus())) {
            throw new RuntimeException("Only DRAFT transfers can be completed");
        }

        t.setStatus("COMPLETED");
        t.setCompletionDate(LocalDate.now());

        /*  create one OUT tx from the "from" location
            and one IN tx into the "to" location                  */
        for (TransferLine l : t.getLines()) {

            Double qty   = (l.getQuantity() != null) ? l.getQuantity() : 0.0;
            UnitOfMeasure uom = l.getUnitOfMeasure();

            if (l.getInventoryItem() != null) {
                InventoryItem item = l.getInventoryItem();

                stockTransactionService.recordTransferOut(
                        t.getFromLocation(), item, qty, uom,
                        t.getId(), t.getCompletionDate());

                stockTransactionService.recordTransferIn(
                        t.getToLocation(),   item, qty, uom,
                        t.getId(), t.getCompletionDate());

            } else {
                SubRecipe prep = l.getSubRecipe();

                stockTransactionService.recordTransferOut(
                        t.getFromLocation(), prep, qty, uom,
                        t.getId(), t.getCompletionDate());

                stockTransactionService.recordTransferIn(
                        t.getToLocation(),   prep, qty, uom,
                        t.getId(), t.getCompletionDate());
            }
        }

        return transferRepository.save(t);
    }


    @Override
    public Transfer getTransfer(Long transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found: " + transferId));
    }

    @Override
    public void deleteTransfer(Long transferId) {
        Transfer transfer = getTransfer(transferId);
        if ("COMPLETED".equalsIgnoreCase(transfer.getStatus())) {
            // remove the stock transactions
            stockTransactionService.deleteBySourceReferenceId(transferId);
        }
        transferRepository.delete(transfer);
    }


    /* in TransferServiceImpl --------------------------------------------- */
    @Override
    public Transfer findDraftBetween(Long from, Long to) {
        return transferRepository
                .findFirstByFromLocationIdAndToLocationIdAndStatus(
                        from, to, "DRAFT").orElse(null);
    }

    @Override
    @Transactional
    public Transfer updateDraftWithLines(Transfer draft,
                                         List<RedistributeJob.ShortLine> lines,
                                         String comment) {

        /* 🔑  Re‑attach + initialise lines */
        draft = transferRepository.findByIdWithLines(draft.getId())
                .orElseThrow(() -> new RuntimeException("Draft not found"));


        /* Map existing lines by (item‑id) so we can merge quantities */
        Map<Long, TransferLine> existing = new HashMap<>();
        for (TransferLine tl : draft.getLines()) {
            if (tl.getInventoryItem()!=null) {
                existing.put(tl.getInventoryItem().getId(), tl);
            }
        }

        for (RedistributeJob.ShortLine sl : lines) {
            if (existing.containsKey(sl.itemId())) {
                TransferLine tl = existing.get(sl.itemId());
                double newQty = tl.getQuantity() + sl.qty();
                tl.setQuantity(newQty);
            } else {
                /* brand‑new line                                               */
                TransferLine tl = new TransferLine();
                tl.setTransfer(draft);
                tl.setInventoryItem(
                        inventoryItemRepository.findById(sl.itemId())
                                .orElseThrow(() -> new RuntimeException("Item not found")));
                tl.setUnitOfMeasure(sl.uom());
                tl.setQuantity(sl.qty());
                draft.getLines().add(tl);
            }
        }

        draft.setStatus("DRAFT");
        draft.setCreationDate(LocalDate.now());
        draft.setComments(
                (draft.getComments()!=null?draft.getComments():"")+
                        " ; "+comment);

        return transferRepository.save(draft);
    }


}
