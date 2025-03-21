package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.SubRecipeItemLineMapper;
import com.rayvision.inventory_management.model.SubRecipeItem;
import com.rayvision.inventory_management.model.dto.SubRecipeItemLineDTO;
import com.rayvision.inventory_management.service.SubRecipeItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sub-recipes/{subRecipeId}/items")
public class SubRecipeItemController {

    private final SubRecipeItemService subRecipeItemService;
    private final SubRecipeItemLineMapper subRecipeItemLineMapper;

    public SubRecipeItemController(SubRecipeItemService subRecipeItemService, SubRecipeItemLineMapper subRecipeItemLineMapper) {
        this.subRecipeItemService = subRecipeItemService;
        this.subRecipeItemLineMapper = subRecipeItemLineMapper;
    }

    // GET all items for the subRecipe
    @GetMapping
    public ResponseEntity<List<SubRecipeItemLineDTO>> getAllItems(@PathVariable Long subRecipeId) {
        List<SubRecipeItem> entities = subRecipeItemService.getItemsBySubRecipe(subRecipeId);
        List<SubRecipeItemLineDTO> dtos = entities.stream()
                .map(subRecipeItemLineMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }


    // GET single item
    @GetMapping("/{itemId}")
    public ResponseEntity<SubRecipeItemLineDTO> getItem(@PathVariable Long subRecipeId,
                                                        @PathVariable Long itemId) {
        return subRecipeItemService.getOne(subRecipeId, itemId)
                .map(subRecipeItemLineMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    // CREATE new subRecipeItem
    @PostMapping
    public ResponseEntity<SubRecipeItemLineDTO> createItem(@PathVariable Long subRecipeId,
                                                           @RequestBody SubRecipeItemLineDTO dto) {
        SubRecipeItem entity = subRecipeItemLineMapper.toEntity(dto);
        SubRecipeItem created = subRecipeItemService.createItem(subRecipeId, entity);
        SubRecipeItemLineDTO responseDto = subRecipeItemLineMapper.toDto(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }


    // UPDATE (full) subRecipeItem
    @PutMapping("/{itemId}")
    public ResponseEntity<SubRecipeItemLineDTO> updateItem(@PathVariable Long subRecipeId,
                                                           @PathVariable Long itemId,
                                                           @RequestBody SubRecipeItemLineDTO dto) {
        dto.setId(itemId);
        SubRecipeItem entity = subRecipeItemLineMapper.toEntity(dto);
        try {
            SubRecipeItem updated = subRecipeItemService.updateItem(subRecipeId, entity);
            return ResponseEntity.ok(subRecipeItemLineMapper.toDto(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }


    // PARTIAL update
    @PatchMapping("/{itemId}")
    public ResponseEntity<SubRecipeItemLineDTO> partialUpdateItem(@PathVariable Long subRecipeId,
                                                                  @PathVariable Long itemId,
                                                                  @RequestBody SubRecipeItemLineDTO dto) {
        dto.setId(itemId);
        SubRecipeItem entity = subRecipeItemLineMapper.toEntity(dto);
        try {
            SubRecipeItem updated = subRecipeItemService.partialUpdateItem(subRecipeId, entity);
            return ResponseEntity.ok(subRecipeItemLineMapper.toDto(updated));
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
