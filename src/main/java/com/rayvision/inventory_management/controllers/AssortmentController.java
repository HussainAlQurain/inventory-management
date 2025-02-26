package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.Assortment;
import com.rayvision.inventory_management.model.dto.AssortmentDTO;
import com.rayvision.inventory_management.service.AssortmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/companies/{companyId}/assortments")
@RestController
public class AssortmentController {
    private final AssortmentService assortmentService;

    public AssortmentController(AssortmentService assortmentService) {
        this.assortmentService = assortmentService;
    }

    // GET all for a company
    @GetMapping
    public ResponseEntity<List<Assortment>> getAll(@PathVariable Long companyId) {
        List<Assortment> list = assortmentService.getAll(companyId);
        return ResponseEntity.ok(list);
    }

    // GET one
    @GetMapping("/{assortmentId}")
    public ResponseEntity<Assortment> getOne(@PathVariable Long companyId,
                                             @PathVariable Long assortmentId) {
        Assortment assortment = assortmentService.getOne(companyId, assortmentId);
        return ResponseEntity.ok(assortment);
    }

    // CREATE
    @PostMapping
    public ResponseEntity<Assortment> create(@PathVariable Long companyId,
                                             @RequestBody AssortmentDTO dto) {
        // if you want to ignore dto.companyId or ensure it matches companyId
        Assortment created = assortmentService.create(companyId, dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // UPDATE (full)
    @PutMapping("/{assortmentId}")
    public ResponseEntity<Assortment> update(@PathVariable Long companyId,
                                             @PathVariable Long assortmentId,
                                             @RequestBody AssortmentDTO dto) {
        Assortment updated = assortmentService.update(companyId, assortmentId, dto);
        return ResponseEntity.ok(updated);
    }

    // PARTIAL
    @PatchMapping("/{assortmentId}")
    public ResponseEntity<Assortment> partialUpdate(@PathVariable Long companyId,
                                                    @PathVariable Long assortmentId,
                                                    @RequestBody AssortmentDTO dto) {
        Assortment updated = assortmentService.partialUpdate(companyId, assortmentId, dto);
        return ResponseEntity.ok(updated);
    }

    // DELETE
    @DeleteMapping("/{assortmentId}")
    public ResponseEntity<Void> delete(@PathVariable Long companyId,
                                       @PathVariable Long assortmentId) {
        assortmentService.delete(companyId, assortmentId);
        return ResponseEntity.noContent().build();
    }

}
