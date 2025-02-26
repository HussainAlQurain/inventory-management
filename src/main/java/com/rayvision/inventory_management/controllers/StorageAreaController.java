package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.StorageAreaMapper;
import com.rayvision.inventory_management.model.StorageArea;
import com.rayvision.inventory_management.model.dto.StorageAreaDTO;
import com.rayvision.inventory_management.service.StorageAreaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/locations/{locationId}/storage-areas")
@RestController
public class StorageAreaController {
    private final StorageAreaService storageAreaService;
    private final StorageAreaMapper storageAreaMapper;

    public StorageAreaController(StorageAreaService storageAreaService,
                                 StorageAreaMapper storageAreaMapper) {
        this.storageAreaService = storageAreaService;
        this.storageAreaMapper = storageAreaMapper;
    }

    // GET all
    @GetMapping
    public ResponseEntity<List<StorageAreaDTO>> getAll(@PathVariable Long locationId) {
        List<StorageArea> areas = storageAreaService.getAllByLocation(locationId);
        List<StorageAreaDTO> dtos = areas.stream()
                .map(storageAreaMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // GET one
    @GetMapping("/{storageAreaId}")
    public ResponseEntity<StorageAreaDTO> getOne(@PathVariable Long locationId,
                                                 @PathVariable Long storageAreaId) {
        StorageArea area = storageAreaService.getOne(locationId, storageAreaId);
        StorageAreaDTO dto = storageAreaMapper.toDto(area);
        return ResponseEntity.ok(dto);
    }

    // CREATE
    @PostMapping
    public ResponseEntity<StorageAreaDTO> create(@PathVariable Long locationId,
                                                 @RequestBody StorageAreaDTO dto) {
        StorageArea created = storageAreaService.create(locationId, dto);
        return new ResponseEntity<>(storageAreaMapper.toDto(created), HttpStatus.CREATED);
    }

    // UPDATE (put or patch, your choice)
    @PutMapping("/{storageAreaId}")
    public ResponseEntity<StorageAreaDTO> update(@PathVariable Long locationId,
                                                 @PathVariable Long storageAreaId,
                                                 @RequestBody StorageAreaDTO dto) {
        StorageArea updated = storageAreaService.update(locationId, storageAreaId, dto);
        return ResponseEntity.ok(storageAreaMapper.toDto(updated));
    }

    // DELETE
    @DeleteMapping("/{storageAreaId}")
    public ResponseEntity<Void> delete(@PathVariable Long locationId,
                                       @PathVariable Long storageAreaId) {
        storageAreaService.delete(locationId, storageAreaId);
        return ResponseEntity.noContent().build();
    }

}
