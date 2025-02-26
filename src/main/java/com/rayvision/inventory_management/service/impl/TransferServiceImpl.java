package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.Location;
import com.rayvision.inventory_management.model.Transfer;
import com.rayvision.inventory_management.model.TransferLine;
import com.rayvision.inventory_management.model.dto.TransferCreateDTO;
import com.rayvision.inventory_management.model.dto.TransferLineDTO;
import com.rayvision.inventory_management.repository.InventoryItemRepository;
import com.rayvision.inventory_management.repository.LocationRepository;
import com.rayvision.inventory_management.repository.TransferRepository;
import com.rayvision.inventory_management.service.StockTransactionService;
import com.rayvision.inventory_management.service.TransferService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class TransferServiceImpl implements TransferService {
    private final TransferRepository transferRepository;
    private final LocationRepository locationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockTransactionService stockTransactionService;

    public TransferServiceImpl(TransferRepository transferRepository,
                               LocationRepository locationRepository,
                               InventoryItemRepository inventoryItemRepository,
                               StockTransactionService stockTransactionService) {
        this.transferRepository = transferRepository;
        this.locationRepository = locationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockTransactionService = stockTransactionService;
    }

    @Override
    public Transfer createTransfer(TransferCreateDTO dto) {
        // 1) fetch fromLocation, toLocation
        Location fromLoc = locationRepository.findById(dto.getFromLocationId())
                .orElseThrow(() -> new RuntimeException("From location not found"));
        Location toLoc = locationRepository.findById(dto.getToLocationId())
                .orElseThrow(() -> new RuntimeException("To location not found"));

        // 2) build Transfer in DRAFT
        Transfer transfer = new Transfer();
        transfer.setCreationDate(LocalDate.now());
        transfer.setStatus("DRAFT");
        transfer.setFromLocation(fromLoc);
        transfer.setToLocation(toLoc);

        // 3) build lines
        List<TransferLine> lines = new ArrayList<>();
        for (TransferLineDTO lineDto : dto.getLines()) {
            InventoryItem item = inventoryItemRepository.findById(lineDto.getInventoryItemId())
                    .orElseThrow(() -> new RuntimeException("InventoryItem not found: " + lineDto.getInventoryItemId()));

            TransferLine line = new TransferLine();
            line.setTransfer(transfer);
            line.setItem(item);
            line.setQuantity(lineDto.getQuantity());
            line.setCostPerUnit(lineDto.getCostPerUnit());
            if (lineDto.getCostPerUnit() != null && lineDto.getQuantity() != null) {
                line.setTotalCost(lineDto.getQuantity() * lineDto.getCostPerUnit());
            } else {
                line.setTotalCost(0.0);
            }

            lines.add(line);
        }
        transfer.setLines(lines);

        return transferRepository.save(transfer);
    }

    @Override
    public Transfer completeTransfer(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found: " + transferId));
        if (!"DRAFT".equalsIgnoreCase(transfer.getStatus())) {
            throw new RuntimeException("Only DRAFT transfers can be completed");
        }

        // 1) set status=COMPLETED, completionDate
        transfer.setStatus("COMPLETED");
        transfer.setCompletionDate(LocalDate.now());

        // 2) record stock transactions:
        //  - recordTransferOut(fromLocation, item, quantity, cost, transferId, date)
        //  - recordTransferIn(toLocation, item, quantity, cost, transferId, date)
        for (TransferLine line : transfer.getLines()) {
            double qty = line.getQuantity() != null ? line.getQuantity() : 0.0;
            double cost = (line.getTotalCost() != null) ? line.getTotalCost() : 0.0;

            // negative from fromLocation
            stockTransactionService.recordTransferOut(
                    transfer.getFromLocation(),
                    line.getItem(),
                    qty,
                    cost,
                    transfer.getId(),
                    transfer.getCompletionDate()
            );

            // positive to toLocation
            stockTransactionService.recordTransferIn(
                    transfer.getToLocation(),
                    line.getItem(),
                    qty,
                    cost,
                    transfer.getId(),
                    transfer.getCompletionDate()
            );
        }

        return transferRepository.save(transfer);
    }

    @Override
    public Transfer getTransfer(Long transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found: " + transferId));
    }

    @Override
    public void deleteTransfer(Long transferId) {
        Transfer transfer = getTransfer(transferId);
        // If it's COMPLETED, we might want to remove the stock transactions or throw an error
        if ("COMPLETED".equalsIgnoreCase(transfer.getStatus())) {
            // revert transactions
            stockTransactionService.deleteBySourceReferenceId(transferId);
        }

        transferRepository.delete(transfer);
    }

}
