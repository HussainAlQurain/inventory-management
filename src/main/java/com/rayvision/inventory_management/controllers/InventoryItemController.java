package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.exceptions.ResourceNotFoundException;
import com.rayvision.inventory_management.facade.InventoryItemFacade;
import com.rayvision.inventory_management.mappers.InventoryItemResponseMapper;
import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.dto.*;
import com.rayvision.inventory_management.service.InventoryItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        return inventoryItemService.getInventoryItemById(companyId, id)
                .map(item -> ResponseEntity.ok(inventoryItemResponseMapper.toInventoryItemResponseDTO(item)))
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "id", id));
    }

    // POST a new inventory item
    @PostMapping("company/{companyId}")
    public ResponseEntity<InventoryItemResponseDTO> createInventoryItem(
            @PathVariable Long companyId,
            @Valid @RequestBody InventoryItemCreateDTO inventoryItemCreateDTO) {

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

    /**
     * New endpoint for paginated inventory items
     * GET /inventory-items/company/{companyId}/paginated?page=0&size=10&sort=name,asc&search=apple
     */
    @Deprecated
    @GetMapping("/company/{companyId}/paginated")
    public ResponseEntity<PageResponseDTO<InventoryItemResponseDTO>> getPaginatedInventoryItems(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) Long categoryId) {
        
        // Create sorting if provided
        Sort sorting = Sort.unsorted();
        if (sort != null && !sort.isEmpty()) {
            String[] sortParams = sort.split(",");
            String sortField = sortParams[0];
            Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            sorting = Sort.by(direction, sortField);
        } else {
            // Default sort by name ascending
            sorting = Sort.by(Sort.Direction.ASC, "name");
        }
        
        Pageable pageable = PageRequest.of(page, size, sorting);
        
        Page<InventoryItem> itemsPage;
        if (categoryId != null) {
            itemsPage = inventoryItemService.findByCompanyIdAndCategoryWithSearch(companyId, categoryId, search, pageable);
        } else {
            itemsPage = inventoryItemService.searchItemsPaginated(companyId, search, pageable);
        }
        
        Page<InventoryItemResponseDTO> dtoPage = itemsPage.map(inventoryItemResponseMapper::toInventoryItemResponseDTO);
        
        PageResponseDTO<InventoryItemResponseDTO> response = new PageResponseDTO<>(
                dtoPage.getContent(),
                dtoPage.getTotalElements(),
                dtoPage.getTotalPages(),
                dtoPage.getNumber(),
                dtoPage.getSize(),
                dtoPage.hasNext(),
                dtoPage.hasPrevious()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Optimized endpoint for paginated inventory items - uses lightweight DTOs to avoid N+1 queries
     */
    @GetMapping("/company/{companyId}/paginated-list")
    public ResponseEntity<PageResponseDTO<InventoryItemListDTO>> getInventoryItemsList(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) Long categoryId) {

        // Create sorting if provided
        Sort sorting = Sort.unsorted();
        if (sort != null && !sort.isEmpty()) {
            String[] sortParams = sort.split(",");
            String sortField = sortParams[0];
            Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            sorting = Sort.by(direction, sortField);
        } else {
            // Default sort by name ascending
            sorting = Sort.by(Sort.Direction.ASC, "name");
        }

        Pageable pageable = PageRequest.of(page, size, sorting);

        // Use the optimized query that returns lightweight DTOs
        Page<InventoryItemListDTO> itemsPage = inventoryItemService.findInventoryItemsForListView(
                companyId, categoryId, search, pageable);

        PageResponseDTO<InventoryItemListDTO> response = new PageResponseDTO<>(
                itemsPage.getContent(),
                itemsPage.getTotalElements(),
                itemsPage.getTotalPages(),
                itemsPage.getNumber(),
                itemsPage.getSize(),
                itemsPage.hasNext(),
                itemsPage.hasPrevious()
        );

        return ResponseEntity.ok(response);
    }
}
