package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.MenuItemResponseMapper;
import com.rayvision.inventory_management.model.MenuItem;
import com.rayvision.inventory_management.model.dto.MenuItemCreateDTO;
import com.rayvision.inventory_management.model.dto.MenuItemInventoryLineDTO;
import com.rayvision.inventory_management.model.dto.MenuItemResponseDTO;
import com.rayvision.inventory_management.service.MenuItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/menu-items")
public class MenuItemController {

    private final MenuItemService menuItemService;
    private final MenuItemResponseMapper menuItemResponseMapper;

    public MenuItemController(MenuItemService menuItemService, MenuItemResponseMapper menuItemResponseMapper) {
        this.menuItemService = menuItemService;
        this.menuItemResponseMapper = menuItemResponseMapper;
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<MenuItemResponseDTO>> getAllMenuItems(
            @PathVariable Long companyId,
            @RequestParam(name = "search", required = false, defaultValue = "") String searchTerm
    ) {
        List<MenuItem> items = menuItemService.searchMenuItems(companyId, searchTerm);
        List<MenuItemResponseDTO> dtoList = items.stream()
                .map(menuItemResponseMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtoList);
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
}
