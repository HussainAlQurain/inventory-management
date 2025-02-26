package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.Transfer;
import com.rayvision.inventory_management.model.dto.TransferCreateDTO;
import com.rayvision.inventory_management.service.TransferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transfers")
public class TransferController {
    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<Transfer> create(@RequestBody TransferCreateDTO dto) {
        Transfer created = transferService.createTransfer(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // COMPLETE
    @PostMapping("/{transferId}/complete")
    public ResponseEntity<Transfer> complete(@PathVariable Long transferId) {
        Transfer completed = transferService.completeTransfer(transferId);
        return ResponseEntity.ok(completed);
    }

    // GET
    @GetMapping("/{transferId}")
    public ResponseEntity<Transfer> getOne(@PathVariable Long transferId) {
        Transfer transfer = transferService.getTransfer(transferId);
        return ResponseEntity.ok(transfer);
    }

    // DELETE
    @DeleteMapping("/{transferId}")
    public ResponseEntity<Void> delete(@PathVariable Long transferId) {
        transferService.deleteTransfer(transferId);
        return ResponseEntity.noContent().build();
    }

}
