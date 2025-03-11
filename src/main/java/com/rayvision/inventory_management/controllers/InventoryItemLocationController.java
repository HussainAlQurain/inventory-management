package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.InventoryItemLocationMapper;
import com.rayvision.inventory_management.model.InventoryItemLocation;
import com.rayvision.inventory_management.model.dto.InventoryItemLocationDTO;
import com.rayvision.inventory_management.service.InventoryItemLocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory-item-locations")
public class InventoryItemLocationController {
    private final InventoryItemLocationService service;
    private final InventoryItemLocationMapper mapper;

    public InventoryItemLocationController(InventoryItemLocationService service,
                                           InventoryItemLocationMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<InventoryItemLocationDTO> create(@RequestBody InventoryItemLocationDTO dto) {
        InventoryItemLocation created = service.create(dto);
        return new ResponseEntity<>(mapper.toDto(created), HttpStatus.CREATED);
    }

    // GET
    @GetMapping("/{id}")
    public ResponseEntity<InventoryItemLocationDTO> getOne(@PathVariable Long id) {
        InventoryItemLocation entity = service.getOne(id)
                .orElseThrow(() -> new RuntimeException("Not found bridging with id=" + id));
        return ResponseEntity.ok(mapper.toDto(entity));
    }

    // UPDATE
    @PatchMapping("/{id}")
    public ResponseEntity<InventoryItemLocationDTO> partialUpdate(@PathVariable Long id,
                                                                  @RequestBody InventoryItemLocationDTO dto) {
        InventoryItemLocation updated = service.update(id, dto);
        return ResponseEntity.ok(mapper.toDto(updated));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}
