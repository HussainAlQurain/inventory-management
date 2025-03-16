package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.InventoryItemLocationMapper;
import com.rayvision.inventory_management.model.InventoryItemLocation;
import com.rayvision.inventory_management.model.dto.InventoryItemLocationDTO;
import com.rayvision.inventory_management.model.records.BulkUpdateRequest;
import com.rayvision.inventory_management.service.InventoryItemLocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory-item-locations")
public class InventoryItemLocationController {
    private final InventoryItemLocationService service;
    private final InventoryItemLocationMapper inventoryItemLocationMapper;

    public InventoryItemLocationController(InventoryItemLocationService service,
                                           InventoryItemLocationMapper inventoryItemLocationMapper) {
        this.service = service;
        this.inventoryItemLocationMapper = inventoryItemLocationMapper;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<InventoryItemLocationDTO> create(@RequestBody InventoryItemLocationDTO dto) {
        InventoryItemLocation created = service.create(dto);
        return new ResponseEntity<>(inventoryItemLocationMapper.toDto(created), HttpStatus.CREATED);
    }

    // GET
    @GetMapping("/{id}")
    public ResponseEntity<InventoryItemLocationDTO> getOne(@PathVariable Long id) {
        InventoryItemLocation entity = service.getOne(id)
                .orElseThrow(() -> new RuntimeException("Not found bridging with id=" + id));
        return ResponseEntity.ok(inventoryItemLocationMapper.toDto(entity));
    }

    // UPDATE
    @PatchMapping("/{id}")
    public ResponseEntity<InventoryItemLocationDTO> partialUpdate(@PathVariable Long id,
                                                                  @RequestBody InventoryItemLocationDTO dto) {
        InventoryItemLocation updated = service.update(id, dto);
        return ResponseEntity.ok(inventoryItemLocationMapper.toDto(updated));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-update")
    public ResponseEntity<Void> bulkUpdate(@RequestBody BulkUpdateRequest req) {
        // example request = { "companyId": 123, "itemId": 456, "newMin": 10, "newPar": 20 }
        service.bulkUpdate(req.companyId(), req.itemId(), req.newMin(), req.newPar());
        return ResponseEntity.ok().build();
    }

}
