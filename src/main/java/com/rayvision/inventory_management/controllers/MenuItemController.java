package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.MenuItemLineMapper;
import com.rayvision.inventory_management.mappers.MenuItemLineMapperImpl;
import com.rayvision.inventory_management.mappers.MenuItemResponseMapper;
import com.rayvision.inventory_management.model.MenuItem;
import com.rayvision.inventory_management.model.MenuItemLine;
import com.rayvision.inventory_management.model.dto.*;
import com.rayvision.inventory_management.service.MenuItemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/menu-items")
public class MenuItemController {

    private final MenuItemService menuItemService;
    private final MenuItemResponseMapper menuItemResponseMapper;
    private final MenuItemLineMapper menuItemLineMapper;

    public MenuItemController(MenuItemService menuItemService, MenuItemResponseMapper menuItemResponseMapper, MenuItemLineMapper menuItemLineMapper) {
        this.menuItemService = menuItemService;
        this.menuItemResponseMapper = menuItemResponseMapper;
        this.menuItemLineMapper = menuItemLineMapper;
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<Map<String, Object>> getAllMenuItems(
            @PathVariable Long companyId,
            @RequestParam(name = "search", required = false, defaultValue = "") String searchTerm,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "name") String sortBy,
            @RequestParam(name = "direction", defaultValue = "asc") String direction
    ) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<MenuItem> pageItems = menuItemService.searchMenuItems(companyId, searchTerm, pageable);
        
        List<MenuItemResponseDTO> items = pageItems.getContent().stream()
                .map(menuItemResponseMapper::toDto)
                .toList();
        
        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("currentPage", pageItems.getNumber());
        response.put("totalItems", pageItems.getTotalElements());
        response.put("totalPages", pageItems.getTotalPages());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/company/{companyId}")
    public ResponseEntity<MenuItemResponseDTO> getMenuItemById(@PathVariable Long companyId,
                                                               @PathVariable Long id) {
        return menuItemService.getMenuItemById(companyId, id)
                .map(menuItemResponseMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/company/{companyId}")
    public ResponseEntity<MenuItemResponseDTO> createMenuItem(@PathVariable Long companyId,
                                                              @RequestBody MenuItemCreateDTO dto) {
        MenuItem created = menuItemService.createMenuItem(companyId, dto);
        MenuItemResponseDTO response = menuItemResponseMapper.toDto(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/company/{companyId}")
    public ResponseEntity<MenuItemResponseDTO> updateMenuItem(@PathVariable Long companyId,
                                                              @PathVariable Long id,
                                                              @RequestBody MenuItemCreateDTO dto) {
        try {
            MenuItem updated = menuItemService.updateMenuItem(companyId, id, dto);
            return ResponseEntity.ok(menuItemResponseMapper.toDto(updated));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/company/{companyId}")
    public ResponseEntity<MenuItemResponseDTO> partialUpdateMenuItem(@PathVariable Long companyId,
                                                                     @PathVariable Long id,
                                                                     @RequestBody MenuItemCreateDTO dto) {
        try {
            MenuItem updated = menuItemService.partialUpdateMenuItem(companyId, id, dto);
            return ResponseEntity.ok(menuItemResponseMapper.toDto(updated));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/company/{companyId}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long companyId,
                                               @PathVariable Long id) {
        try {
            menuItemService.deleteMenuItemById(companyId, id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    // Line management endpoints
    @GetMapping("/{menuItemId}/company/{companyId}/lines")
    public ResponseEntity<List<MenuItemLineResponseDTO>> getMenuItemLines(
            @PathVariable Long companyId,
            @PathVariable Long menuItemId) {
        MenuItem menuItem = menuItemService.getMenuItemById(companyId, menuItemId)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));
        return ResponseEntity.ok(menuItemLineMapper.toDtoList(menuItem.getMenuItemLines()));
    }

    @PostMapping("/{menuItemId}/company/{companyId}/lines")
    public ResponseEntity<MenuItemLineResponseDTO> addLineToMenuItem(
            @PathVariable Long companyId,
            @PathVariable Long menuItemId,
            @RequestBody MenuItemLineDTO dto) {
        MenuItem updated = menuItemService.addLineToMenuItem(companyId, menuItemId, dto);
        MenuItemLine newLine = updated.getMenuItemLines().stream()
                .filter(line -> line.getId() != null)
                .reduce((first, second) -> second)
                .orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(menuItemLineMapper.toDto(newLine));
    }

    @PutMapping("/{menuItemId}/company/{companyId}/lines/{lineId}")
    public ResponseEntity<MenuItemLineResponseDTO> updateLine(
            @PathVariable Long companyId,
            @PathVariable Long menuItemId,
            @PathVariable Long lineId,
            @RequestBody MenuItemLineDTO dto) {
        dto.setId(lineId);
        MenuItem updated = menuItemService.updateLine(companyId, menuItemId, dto);
        return ResponseEntity.ok(menuItemLineMapper.toDto(findLine(updated, lineId)));
    }

    @DeleteMapping("/{menuItemId}/company/{companyId}/lines/{lineId}")
    public ResponseEntity<Void> removeLineFromMenuItem(
            @PathVariable Long companyId,
            @PathVariable Long menuItemId,
            @PathVariable Long lineId) {
        menuItemService.removeLineFromMenuItem(companyId, menuItemId, lineId);
        return ResponseEntity.noContent().build();
    }

    private MenuItemLine findLine(MenuItem menuItem, Long lineId) {
        return menuItem.getMenuItemLines().stream()
                .filter(line -> line.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Line not found"));
    }

}
