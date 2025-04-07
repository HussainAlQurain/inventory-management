package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.TransferCreateDTO;
import com.rayvision.inventory_management.model.dto.TransferLineDTO;
import com.rayvision.inventory_management.repository.InventoryItemRepository;
import com.rayvision.inventory_management.repository.LocationRepository;
import com.rayvision.inventory_management.repository.TransferRepository;
import com.rayvision.inventory_management.repository.UnitOfMeasureRepository;
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
    private final UnitOfMeasureRepository uomRepository;   // so we can fetch the user’s chosen UOM
    private final StockTransactionService stockTransactionService;

    public TransferServiceImpl(TransferRepository transferRepository,
                               LocationRepository locationRepository,
                               InventoryItemRepository inventoryItemRepository,
                               StockTransactionService stockTransactionService,
                               UnitOfMeasureRepository uomRepository) {
        this.transferRepository = transferRepository;
        this.locationRepository = locationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockTransactionService = stockTransactionService;
        this.uomRepository = uomRepository;
    }

    @Override
    public Transfer createTransfer(TransferCreateDTO dto) {
        Location fromLoc = locationRepository.findById(dto.getFromLocationId())
                .orElseThrow(() -> new RuntimeException("From location not found"));
        Location toLoc = locationRepository.findById(dto.getToLocationId())
                .orElseThrow(() -> new RuntimeException("To location not found"));

        Transfer transfer = new Transfer();
        transfer.setCreationDate(LocalDate.now());
        transfer.setStatus("DRAFT");
        transfer.setFromLocation(fromLoc);
        transfer.setToLocation(toLoc);

        List<TransferLine> lines = new ArrayList<>();
        for (TransferLineDTO lineDto : dto.getLines()) {
            InventoryItem item = inventoryItemRepository.findById(lineDto.getInventoryItemId())
                    .orElseThrow(() -> new RuntimeException("InventoryItem not found: " + lineDto.getInventoryItemId()));

            TransferLine line = new TransferLine();
            line.setTransfer(transfer);
            line.setItem(item);
            line.setQuantity(lineDto.getQuantity());

            // optional cost
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

        transfer.setStatus("COMPLETED");
        transfer.setCompletionDate(LocalDate.now());

        // Record stock transactions for each line
        for (TransferLine line : transfer.getLines()) {
            double qty = (line.getQuantity() != null) ? line.getQuantity() : 0.0;

            // Now we also need the user’s chosen UOM ID to do the conversion.
            // You might store it in TransferLine if you want. For example:
            //   lineDto had "unitOfMeasureId"
            //   or line has a field 'uomId' or something
            // But in your code snippet, it doesn't exist. So:
            // EITHER we assume the line's itemUom is the chosen UOM:
            UnitOfMeasure defaultUom = line.getItem().getInventoryUom();

            // Outflow from fromLocation
            stockTransactionService.recordTransferOut(
                    transfer.getFromLocation(),
                    line.getItem(),
                    qty,
                    defaultUom,       // user-chosen or item default
                    transfer.getId(),
                    transfer.getCompletionDate()
            );

            // Inflow to toLocation
            stockTransactionService.recordTransferIn(
                    transfer.getToLocation(),
                    line.getItem(),
                    qty,
                    defaultUom,
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
        if ("COMPLETED".equalsIgnoreCase(transfer.getStatus())) {
            // remove the stock transactions
            stockTransactionService.deleteBySourceReferenceId(transferId);
        }
        transferRepository.delete(transfer);
    }


}
