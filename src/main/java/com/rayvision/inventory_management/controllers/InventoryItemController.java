package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.facade.InventoryItemFacade;
import com.rayvision.inventory_management.mappers.InventoryItemResponseMapper;
import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.dto.InventoryItemCreateDTO;
import com.rayvision.inventory_management.model.dto.InventoryItemPartialUpdateDTO;
import com.rayvision.inventory_management.model.dto.InventoryItemResponseDTO;
import com.rayvision.inventory_management.service.InventoryItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inventory-items")
public class InventoryItemController {
    private final InventoryItemService inventoryItemService;
    private final InventoryItemFacade inventoryItemFacade;
    private final InventoryItemResponseMapper inventoryItemResponseMapper;

    @Autowired
    public InventoryItemController(InventoryItemService inventoryItemService, InventoryItemFacade inventoryItemFacade, InventoryItemResponseMapper inventoryItemResponseMapper) {
        this.inventoryItemService = inventoryItemService;
        this.inventoryItemFacade = inventoryItemFacade;
        this.inventoryItemResponseMapper = inventoryItemResponseMapper;

    }

    // GET all inventory items
//    @GetMapping("company/{companyId}")
//    public ResponseEntity<List<InventoryItemResponseDTO>> getAllInventoryItems(@PathVariable Long companyId) {
//        List<InventoryItem> items = inventoryItemService.getAllInventoryItems(companyId);
//        List<InventoryItemResponseDTO> responseList = items.stream().map(inventoryItemResponseMapper::toInventoryItemResponseDTO).toList();
//        return ResponseEntity.ok(responseList);
//    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<InventoryItemResponseDTO>> getInventoryItemsByCompany(
            @PathVariable Long companyId,
            @RequestParam(name = "search", required = false, defaultValue = "") String searchTerm
    ) {
        List<InventoryItem> items = inventoryItemService.searchItems(companyId, searchTerm);
        // map to DTO
        List<InventoryItemResponseDTO> dtos = items.stream()
                .map(inventoryItemResponseMapper::toInventoryItemResponseDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // GET a single inventory item by id
    @GetMapping("/{id}/company/{companyId}")
    public ResponseEntity<InventoryItemResponseDTO> getInventoryItemById(@PathVariable Long id, @PathVariable Long companyId) {
        Optional<InventoryItem> itemOpt = inventoryItemService.getInventoryItemById(companyId, id);
        if (itemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        InventoryItemResponseDTO response = inventoryItemResponseMapper.toInventoryItemResponseDTO(itemOpt.get());
        return ResponseEntity.ok(response);
    }

    // POST a new inventory item
    @PostMapping("company/{companyId}")
    public ResponseEntity<InventoryItemResponseDTO> createInventoryItem(@PathVariable Long companyId, @RequestBody InventoryItemCreateDTO inventoryItemCreateDTO) {
        // consider returning inventoryitemdto
        InventoryItem savedItem = inventoryItemFacade.createInventoryItem(companyId, inventoryItemCreateDTO);
        InventoryItemResponseDTO responseDto = inventoryItemResponseMapper.toInventoryItemResponseDTO(savedItem);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    // PUT to update an existing inventory item.
    // Note: This method assumes that the service’s update() method is implemented.
//    @PutMapping("/{id}/company/{companyId}")
//    public ResponseEntity<InventoryItem> updateInventoryItem(@PathVariable Long id, @PathVariable Long companyId,
//                                                             @RequestBody InventoryItem inventoryItem) {
//        inventoryItem.setId(id);
//        InventoryItem updatedItem = inventoryItemService.update(companyId, inventoryItem);
//        if (updatedItem != null) {
//            return ResponseEntity.ok(updatedItem);
//        }
//        return ResponseEntity.notFound().build();
//    }

    // PATCH to partially update an inventory item.
    @PatchMapping("/{id}/company/{companyId}")
    public ResponseEntity<InventoryItemResponseDTO> partialUpdateInventoryItem(
            @PathVariable Long id,
            @PathVariable Long companyId,
            @RequestBody InventoryItemPartialUpdateDTO partialDto
    ) {
        try {
            InventoryItem updated = inventoryItemService.partialUpdate(companyId, id, partialDto);
            InventoryItemResponseDTO response = inventoryItemResponseMapper.toInventoryItemResponseDTO(updated);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE an inventory item by id.
//    @DeleteMapping("/{id}/company/{companyId}")
//    public ResponseEntity<Void> deleteInventoryItem(@PathVariable Long id, @PathVariable Long companyId) {
//        try {
//            inventoryItemService.deleteInventoryItemById(companyId, id);
//            return ResponseEntity.noContent().build();
//        } catch (RuntimeException e) {
//            return ResponseEntity.notFound().build();
//        }
//    }
}
