package com.rayvision.inventory_management.controllers;

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

    public AssortmentController(AssortmentService assortmentService) {
        this.assortmentService = assortmentService;
    }

    // ---------------------------------------------------------
    // GET all for a company
    // ---------------------------------------------------------
    @GetMapping
    public ResponseEntity<List<Assortment>> getAll(@PathVariable Long companyId) {
        List<Assortment> list = assortmentService.getAll(companyId);
        return ResponseEntity.ok(list);
    }

    // ---------------------------------------------------------
    // GET one
    // ---------------------------------------------------------
    @GetMapping("/{assortmentId}")
    public ResponseEntity<Assortment> getOne(@PathVariable Long companyId,
                                             @PathVariable Long assortmentId) {
        Assortment assortment = assortmentService.getOne(companyId, assortmentId);
        return ResponseEntity.ok(assortment);
    }

    @PostMapping("/simple")
    public ResponseEntity<Assortment> createSimple(@PathVariable Long companyId,
                                                   @RequestParam String name) {
        Assortment created = assortmentService.create(companyId, name);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // ---------------------------------------------------------
    // UPDATE (full)
    // ---------------------------------------------------------
    @PutMapping("/{assortmentId}")
    public ResponseEntity<Assortment> update(@PathVariable Long companyId,
                                             @PathVariable Long assortmentId,
                                             @RequestBody AssortmentDTO dto) {
        Assortment updated = assortmentService.update(companyId, assortmentId, dto);
        return ResponseEntity.ok(updated);
    }

    // ---------------------------------------------------------
    // PARTIAL
    // ---------------------------------------------------------
    @PatchMapping("/{assortmentId}")
    public ResponseEntity<Assortment> partialUpdate(@PathVariable Long companyId,
                                                    @PathVariable Long assortmentId,
                                                    @RequestBody AssortmentDTO dto) {
        Assortment updated = assortmentService.partialUpdate(companyId, assortmentId, dto);
        return ResponseEntity.ok(updated);
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
    public ResponseEntity<Assortment> addItems(@PathVariable Long companyId,
                                               @PathVariable Long assortmentId,
                                               @RequestBody BulkIdRequest request) {
        Assortment updated = assortmentService.addInventoryItems(companyId, assortmentId, request.getIds());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{assortmentId}/items/remove")
    public ResponseEntity<Assortment> removeItems(@PathVariable Long companyId,
                                                  @PathVariable Long assortmentId,
                                                  @RequestBody BulkIdRequest request) {
        Assortment updated = assortmentService.removeInventoryItems(companyId, assortmentId, request.getIds());
        return ResponseEntity.ok(updated);
    }

    // ---------------------------------------------------------
    // BULK ADD/REMOVE: SUBRECIPES
    // ---------------------------------------------------------
    @PostMapping("/{assortmentId}/subrecipes/add")
    public ResponseEntity<Assortment> addSubRecipes(@PathVariable Long companyId,
                                                    @PathVariable Long assortmentId,
                                                    @RequestBody BulkIdRequest request) {
        Assortment updated = assortmentService.addSubRecipes(companyId, assortmentId, request.getIds());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{assortmentId}/subrecipes/remove")
    public ResponseEntity<Assortment> removeSubRecipes(@PathVariable Long companyId,
                                                       @PathVariable Long assortmentId,
                                                       @RequestBody BulkIdRequest request) {
        Assortment updated = assortmentService.removeSubRecipes(companyId, assortmentId, request.getIds());
        return ResponseEntity.ok(updated);
    }

    // ---------------------------------------------------------
    // BULK ADD/REMOVE: PURCHASE OPTIONS
    // ---------------------------------------------------------
    @PostMapping("/{assortmentId}/purchaseoptions/add")
    public ResponseEntity<Assortment> addPurchaseOptions(@PathVariable Long companyId,
                                                         @PathVariable Long assortmentId,
                                                         @RequestBody BulkIdRequest request) {
        Assortment updated = assortmentService.addPurchaseOptions(companyId, assortmentId, request.getIds());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{assortmentId}/purchaseoptions/remove")
    public ResponseEntity<Assortment> removePurchaseOptions(@PathVariable Long companyId,
                                                            @PathVariable Long assortmentId,
                                                            @RequestBody BulkIdRequest request) {
        Assortment updated = assortmentService.removePurchaseOptions(companyId, assortmentId, request.getIds());
        return ResponseEntity.ok(updated);
    }

    // ---------------------------------------------------------
    // BULK ADD/REMOVE: LOCATIONS
    // ---------------------------------------------------------
    @PostMapping("/{assortmentId}/locations/add")
    public ResponseEntity<Assortment> addLocations(@PathVariable Long companyId,
                                                   @PathVariable Long assortmentId,
                                                   @RequestBody BulkIdRequest request) {
        Assortment updated = assortmentService.addLocations(companyId, assortmentId, request.getIds());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{assortmentId}/locations/remove")
    public ResponseEntity<Assortment> removeLocations(@PathVariable Long companyId,
                                                      @PathVariable Long assortmentId,
                                                      @RequestBody BulkIdRequest request) {
        Assortment updated = assortmentService.removeLocations(companyId, assortmentId, request.getIds());
        return ResponseEntity.ok(updated);
    }


}
