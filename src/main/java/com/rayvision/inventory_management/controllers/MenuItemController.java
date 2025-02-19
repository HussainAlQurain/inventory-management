package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.MenuItem;
import com.rayvision.inventory_management.model.dto.MenuItemCreateDTO;
import com.rayvision.inventory_management.service.MenuItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/menu-items")
public class MenuItemController {

    private final MenuItemService menuItemService;

    public MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<MenuItem>> getAllMenuItems(@PathVariable Long companyId) {
        return ResponseEntity.ok(menuItemService.getAllMenuItems(companyId));
    }

    @GetMapping("/{id}/company/{companyId}")
    public ResponseEntity<MenuItem> getMenuItemById(@PathVariable Long companyId,
                                                    @PathVariable Long id) {
        return menuItemService.getMenuItemById(companyId, id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/company/{companyId}")
    public ResponseEntity<MenuItem> createMenuItem(@PathVariable Long companyId,
                                                   @RequestBody MenuItemCreateDTO dto) {
        MenuItem created = menuItemService.createMenuItem(companyId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/company/{companyId}")
    public ResponseEntity<MenuItem> updateMenuItem(@PathVariable Long companyId,
                                                   @PathVariable Long id,
                                                   @RequestBody MenuItemCreateDTO dto) {
        try {
            MenuItem updated = menuItemService.updateMenuItem(companyId, id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/company/{companyId}")
    public ResponseEntity<MenuItem> partialUpdateMenuItem(@PathVariable Long companyId,
                                                          @PathVariable Long id,
                                                          @RequestBody MenuItemCreateDTO dto) {
        try {
            MenuItem updated = menuItemService.partialUpdateMenuItem(companyId, id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/company/{companyId}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long companyId,
                                               @PathVariable Long id) {
        try {
            menuItemService.deleteMenuItemById(companyId, id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
