package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.service.InventoryItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/inventory-items")
public class InventoryItemController {
    private final InventoryItemService inventoryItemService;

    @Autowired
    public InventoryItemController(InventoryItemService inventoryItemService) {
        this.inventoryItemService = inventoryItemService;
    }

    // GET all inventory items
    @GetMapping("company/{companyId}")
    public ResponseEntity<List<InventoryItem>> getAllInventoryItems(@PathVariable Long companyId) {
        List<InventoryItem> items = inventoryItemService.getAllInventoryItems(companyId);
        return ResponseEntity.ok(items);
    }

    // GET a single inventory item by id
    @GetMapping("/{id}/company/{companyId}")
    public ResponseEntity<InventoryItem> getInventoryItemById(@PathVariable Long id, @PathVariable Long companyId) {
        Optional<InventoryItem> item = inventoryItemService.getInventoryItemById(companyId, id);
        return item.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // POST a new inventory item
    @PostMapping("company/{companyId}")
    public ResponseEntity<InventoryItem> createInventoryItem(@PathVariable Long companyId, @RequestBody InventoryItem inventoryItem) {
        InventoryItem savedItem = inventoryItemService.save(companyId, inventoryItem);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedItem);
    }

    // PUT to update an existing inventory item.
    // Note: This method assumes that the service’s update() method is implemented.
    @PutMapping("/{id}/company/{companyId}")
    public ResponseEntity<InventoryItem> updateInventoryItem(@PathVariable Long id, @PathVariable Long companyId,
                                                             @RequestBody InventoryItem inventoryItem) {
        inventoryItem.setId(id);
        InventoryItem updatedItem = inventoryItemService.update(companyId, inventoryItem);
        if (updatedItem != null) {
            return ResponseEntity.ok(updatedItem);
        }
        return ResponseEntity.notFound().build();
    }

    // PATCH to partially update an inventory item.
    @PatchMapping("/{id}/company/{companyId}")
    public ResponseEntity<InventoryItem> partialUpdateInventoryItem(@PathVariable Long id, @PathVariable Long companyid,
                                                                    @RequestBody InventoryItem inventoryItem) {
        inventoryItem.setId(id);
        try {
            InventoryItem updatedItem = inventoryItemService.partialUpdate(companyid, inventoryItem);
            return ResponseEntity.ok(updatedItem);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE an inventory item by id.
    @DeleteMapping("/{id}/company/{companyId}")
    public ResponseEntity<Void> deleteInventoryItem(@PathVariable Long id, @PathVariable Long companyId) {
        try {
            inventoryItemService.deleteInventoryItemById(companyId, id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
