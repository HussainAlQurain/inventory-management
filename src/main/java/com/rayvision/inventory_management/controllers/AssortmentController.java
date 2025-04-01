package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.AssortmentMapper;
import com.rayvision.inventory_management.model.Assortment;
import com.rayvision.inventory_management.model.dto.AssortmentDTO;
import com.rayvision.inventory_management.model.dto.BulkIdRequest;
import com.rayvision.inventory_management.service.AssortmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/companies/{companyId}/assortments")
@RestController
public class AssortmentController {
    private final AssortmentService assortmentService;
    private final AssortmentMapper assortmentMapper;

    public AssortmentController(AssortmentService assortmentService, AssortmentMapper assortmentMapper) {
        this.assortmentService = assortmentService;
        this.assortmentMapper = assortmentMapper;
    }

    // ---------------------------------------------------------
    // GET all for a company
    // ---------------------------------------------------------
    @GetMapping
    public ResponseEntity<List<AssortmentDTO>> getAll(@PathVariable Long companyId) {
        List<Assortment> list = assortmentService.getAll(companyId);
        // Convert each to DTO
        List<AssortmentDTO> dtoList = list.stream()
                .map(assortmentMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtoList);
    }

    // ---------------------------------------------------------
    // GET one
    // ---------------------------------------------------------
    @GetMapping("/{assortmentId}")
    public ResponseEntity<AssortmentDTO> getOne(@PathVariable Long companyId,
                                                @PathVariable Long assortmentId) {
        Assortment assortment = assortmentService.getOne(companyId, assortmentId);
        AssortmentDTO dto = assortmentMapper.toDto(assortment);
        return ResponseEntity.ok(dto);
    }

    // ---------------------------------------------------------
    // CREATE a “simple” one with just name
    // ---------------------------------------------------------
    @PostMapping("/simple")
    public ResponseEntity<AssortmentDTO> createSimple(@PathVariable Long companyId,
                                                      @RequestParam String name) {
        Assortment created = assortmentService.create(companyId, name);
        // convert to dto
        AssortmentDTO dto = assortmentMapper.toDto(created);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }


    // ---------------------------------------------------------
    // UPDATE (full)
    // ---------------------------------------------------------
    @PutMapping("/{assortmentId}")
    public ResponseEntity<AssortmentDTO> update(@PathVariable Long companyId,
                                                @PathVariable Long assortmentId,
                                                @RequestBody AssortmentDTO body) {
        Assortment updated = assortmentService.update(companyId, assortmentId, body);
        AssortmentDTO dto = assortmentMapper.toDto(updated);
        return ResponseEntity.ok(dto);
    }

    // ---------------------------------------------------------
    // PARTIAL
    // ---------------------------------------------------------
    @PatchMapping("/{assortmentId}")
    public ResponseEntity<AssortmentDTO> partialUpdate(@PathVariable Long companyId,
                                                       @PathVariable Long assortmentId,
                                                       @RequestBody AssortmentDTO body) {
        Assortment updated = assortmentService.partialUpdate(companyId, assortmentId, body);
        AssortmentDTO dto = assortmentMapper.toDto(updated);
        return ResponseEntity.ok(dto);
    }

    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------
    @DeleteMapping("/{assortmentId}")
    public ResponseEntity<Void> delete(@PathVariable Long companyId,
                                       @PathVariable Long assortmentId) {
        assortmentService.delete(companyId, assortmentId);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------
    // BULK ADD/REMOVE: ITEMS
    // ---------------------------------------------------------
    @PostMapping("/{assortmentId}/items/add")
    public ResponseEntity<AssortmentDTO> addItems(@PathVariable Long companyId,
                                                  @PathVariable Long assortmentId,
                                                  @RequestBody BulkIdRequest request) {
        Assortment updated = assortmentService.addInventoryItems(companyId, assortmentId, request.getIds());
        return ResponseEntity.ok(assortmentMapper.toDto(updated));
    }

    @PostMapping("/{assortmentId}/items/remove")
    public ResponseEntity<AssortmentDTO> removeItems(@PathVariable Long companyId,
                                                     @PathVariable Long assortmentId,
                                                     @RequestBody BulkIdRequest request) {
        Assortment updated = assortmentService.removeInventoryItems(companyId, assortmentId, request.getIds());
        return ResponseEntity.ok(assortmentMapper.toDto(updated));
    }

    // ---------------------------------------------------------
    // BULK ADD/REMOVE: SUBRECIPES
    // ---------------------------------------------------------
    @PostMapping("/{assortmentId}/subrecipes/add")
    public ResponseEntity<AssortmentDTO> addSubRecipes(@PathVariable Long companyId,
                                                       @PathVariable Long assortmentId,
                                                       @RequestBody BulkIdRequest request) {
        Assortment updated = assortmentService.addSubRecipes(companyId, assortmentId, request.getIds());
        return ResponseEntity.ok(assortmentMapper.toDto(updated));
    }

    @PostMapping("/{assortmentId}/subrecipes/remove")
    public ResponseEntity<AssortmentDTO> removeSubRecipes(@PathVariable Long companyId,
                                                          @PathVariable Long assortmentId,
                                                          @RequestBody BulkIdRequest request) {
        Assortment updated = assortmentService.removeSubRecipes(companyId, assortmentId, request.getIds());
        return ResponseEntity.ok(assortmentMapper.toDto(updated));
    }

    // ---------------------------------------------------------
    // BULK ADD/REMOVE: PURCHASE OPTIONS
    // ---------------------------------------------------------
    @PostMapping("/{assortmentId}/purchaseoptions/add")
    public ResponseEntity<AssortmentDTO> addPurchaseOptions(@PathVariable Long companyId,
                                                            @PathVariable Long assortmentId,
                                                            @RequestBody BulkIdRequest request) {
        Assortment updated = assortmentService.addPurchaseOptions(companyId, assortmentId, request.getIds());
        return ResponseEntity.ok(assortmentMapper.toDto(updated));
    }

    @PostMapping("/{assortmentId}/purchaseoptions/remove")
    public ResponseEntity<AssortmentDTO> removePurchaseOptions(@PathVariable Long companyId,
                                                               @PathVariable Long assortmentId,
                                                               @RequestBody BulkIdRequest request) {
        Assortment updated = assortmentService.removePurchaseOptions(companyId, assortmentId, request.getIds());
        return ResponseEntity.ok(assortmentMapper.toDto(updated));
    }

    // ---------------------------------------------------------
    // BULK ADD/REMOVE: LOCATIONS
    // ---------------------------------------------------------
    @PostMapping("/{assortmentId}/locations/add")
    public ResponseEntity<AssortmentDTO> addLocations(@PathVariable Long companyId,
                                                      @PathVariable Long assortmentId,
                                                      @RequestBody BulkIdRequest request) {
        Assortment updated = assortmentService.addLocations(companyId, assortmentId, request.getIds());
        return ResponseEntity.ok(assortmentMapper.toDto(updated));
    }

    @PostMapping("/{assortmentId}/locations/remove")
    public ResponseEntity<AssortmentDTO> removeLocations(@PathVariable Long companyId,
                                                         @PathVariable Long assortmentId,
                                                         @RequestBody BulkIdRequest request) {
        Assortment updated = assortmentService.removeLocations(companyId, assortmentId, request.getIds());
        return ResponseEntity.ok(assortmentMapper.toDto(updated));
    }


}
