package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.TransferMapper;
import com.rayvision.inventory_management.model.Transfer;
import com.rayvision.inventory_management.model.dto.TransferCreateDTO;
import com.rayvision.inventory_management.model.dto.TransferDTO;
import com.rayvision.inventory_management.model.dto.TransferLineDTO;
import com.rayvision.inventory_management.service.TransferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transfers")
public class TransferController {
    private final TransferService transferService;
    private final TransferMapper transferMapper;

    public TransferController(TransferService transferService, TransferMapper transferMapper) {
        this.transferService = transferService;
        this.transferMapper = transferMapper;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<TransferDTO> create(@RequestBody TransferCreateDTO dto) {
        Transfer created = transferService.createTransfer(dto);
        return new ResponseEntity<>(transferMapper.toDto(created), HttpStatus.CREATED);
    }

    // COMPLETE
    @PostMapping("/{id}/complete")
    public ResponseEntity<TransferDTO> complete(@PathVariable Long id) {
        Transfer completed = transferService.completeTransfer(id);
        return ResponseEntity.ok(transferMapper.toDto(completed));
    }

    // GET
    @GetMapping("/{id}")
    public ResponseEntity<TransferDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(transferMapper.toDto(transferService.getTransfer(id)));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transferService.deleteTransfer(id);
        return ResponseEntity.noContent().build();
    }

    /* ----------- LISTS (Location) ------------------------------------- */
    @GetMapping("/location/{locId}/outgoing")
    public List<TransferDTO> outgoingByLoc(@PathVariable Long locId){
        return transferService.findOutgoingDraftsByLocation(locId)
                .stream().map(transferMapper::toDto).toList();
    }

    @GetMapping("/location/{locId}/incoming")
    public List<TransferDTO> incomingByLoc(@PathVariable Long locId){
        return transferService.findIncomingDraftsByLocation(locId)
                .stream().map(transferMapper::toDto).toList();
    }
    
    /* ----------- COMPLETED LISTS (Location) --------------------------- */
    @GetMapping("/location/{locId}/completed")
    public List<TransferDTO> completedByLoc(
            @PathVariable Long locId,
            @RequestParam(required = false, defaultValue = "false") boolean fromLocation) {
        return transferService.findCompletedTransfersByLocation(locId, fromLocation)
                .stream().map(transferMapper::toDto).toList();
    }

    /* ----------- LISTS (Company) -------------------------------------- */
    @GetMapping("/company/{compId}/outgoing")
    public List<TransferDTO> outgoingByCo(@PathVariable Long compId){
        return transferService.findOutgoingDraftsByCompany(compId)
                .stream().map(transferMapper::toDto).toList();
    }

    @GetMapping("/company/{compId}/incoming")
    public List<TransferDTO> incomingByCo(@PathVariable Long compId){
        return transferService.findIncomingDraftsByCompany(compId)
                .stream().map(transferMapper::toDto).toList();
    }
    
    /* ----------- COMPLETED LISTS (Company) ---------------------------- */
    @GetMapping("/company/{compId}/completed")
    public List<TransferDTO> completedByCo(@PathVariable Long compId){
        return transferService.findCompletedTransfersByCompany(compId)
                .stream().map(transferMapper::toDto).toList();
    }
    
    /* ----------- ALL COMPLETED TRANSFERS ------------------------------ */
    @GetMapping("/completed")
    public List<TransferDTO> allCompleted() {
        return transferService.findAllCompletedTransfers()
                .stream().map(transferMapper::toDto).toList();
    }

    /* -------------------- replace lines in draft ---------------------- */
    @PutMapping("/{id}/lines")
    public TransferDTO replaceLines(@PathVariable Long id,
                                    @RequestParam Long actingLocationId,
                                    @RequestBody List<TransferLineDTO> lines){
        Transfer t = transferService.updateDraft(id, lines, actingLocationId);
        return transferMapper.toDto(t);
    }
}
