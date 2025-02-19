package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.model.SubRecipeItem;
import com.rayvision.inventory_management.service.SubRecipeItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sub-recipes/{subRecipeId}/items")
public class SubRecipeItemController {

    private final SubRecipeItemService subRecipeItemService;

    public SubRecipeItemController(SubRecipeItemService subRecipeItemService) {
        this.subRecipeItemService = subRecipeItemService;
    }

    // GET all items for the subRecipe
    @GetMapping
    public ResponseEntity<List<SubRecipeItem>> getAllItems(@PathVariable Long subRecipeId) {
        List<SubRecipeItem> items = subRecipeItemService.getItemsBySubRecipe(subRecipeId);
        return ResponseEntity.ok(items);
    }

    // GET single item
    @GetMapping("/{itemId}")
    public ResponseEntity<SubRecipeItem> getItem(@PathVariable Long subRecipeId,
                                                 @PathVariable Long itemId) {
        return subRecipeItemService.getOne(subRecipeId, itemId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // CREATE new subRecipeItem
    @PostMapping
    public ResponseEntity<SubRecipeItem> createItem(@PathVariable Long subRecipeId,
                                                    @RequestBody SubRecipeItem item) {
        SubRecipeItem created = subRecipeItemService.createItem(subRecipeId, item);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // UPDATE (full) subRecipeItem
    @PutMapping("/{itemId}")
    public ResponseEntity<SubRecipeItem> updateItem(@PathVariable Long subRecipeId,
                                                    @PathVariable Long itemId,
                                                    @RequestBody SubRecipeItem item) {
        item.setId(itemId);
        try {
            SubRecipeItem updated = subRecipeItemService.updateItem(subRecipeId, item);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // PARTIAL update
    @PatchMapping("/{itemId}")
    public ResponseEntity<SubRecipeItem> partialUpdateItem(@PathVariable Long subRecipeId,
                                                           @PathVariable Long itemId,
                                                           @RequestBody SubRecipeItem patchItem) {
        patchItem.setId(itemId);
        try {
            SubRecipeItem updated = subRecipeItemService.partialUpdateItem(subRecipeId, patchItem);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE item
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long subRecipeId,
                                           @PathVariable Long itemId) {
        try {
            subRecipeItemService.deleteItem(subRecipeId, itemId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
