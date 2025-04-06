package com.rayvision.inventory_management.controllers;

import com.rayvision.inventory_management.mappers.CountUomPreferenceMapper;
import com.rayvision.inventory_management.model.CountUomPreference;
import com.rayvision.inventory_management.model.UnitOfMeasure;
import com.rayvision.inventory_management.model.dto.CountUomPreferenceDTO;
import com.rayvision.inventory_management.model.dto.UomResponseDTO;
import com.rayvision.inventory_management.service.CountUomPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/count-uom-preferences")
public class CountUomPreferenceController {
    private final CountUomPreferenceService preferenceService;
    private final CountUomPreferenceMapper preferenceMapper;

    public CountUomPreferenceController(CountUomPreferenceService preferenceService,
                                        CountUomPreferenceMapper preferenceMapper) {
        this.preferenceService = preferenceService;
        this.preferenceMapper = preferenceMapper;
    }

    // ------------------------------------------
    // 1) INVENTORY ITEM endpoints
    // ------------------------------------------

    /**
     * List all preferences for an InventoryItem.
     */
    @GetMapping("/items/{itemId}")
    public ResponseEntity<List<CountUomPreferenceDTO>> getPreferencesForItem(@PathVariable Long itemId) {
        List<CountUomPreference> prefs = preferenceService.getPreferencesForItem(itemId);
        List<CountUomPreferenceDTO> dtos = prefs.stream()
                .map(preferenceMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Add or update a preference for an InventoryItem.
     * e.g. POST /count-uom-preferences/items/100?uomId=5&defaultUom=false
     */
    @PostMapping("/items/{itemId}")
    public ResponseEntity<CountUomPreferenceDTO> addPreferenceForItem(
            @PathVariable Long itemId,
            @RequestParam Long uomId,
            @RequestParam(defaultValue = "false") boolean defaultUom
    ) {
        CountUomPreference pref = preferenceService.addPreferenceForItem(itemId, uomId, defaultUom);
        return ResponseEntity.ok(preferenceMapper.toDto(pref));
    }

    /**
     * Remove a preference for an InventoryItem.
     * e.g. DELETE /count-uom-preferences/items/100?uomId=5
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removePreferenceForItem(
            @PathVariable Long itemId,
            @RequestParam Long uomId
    ) {
        preferenceService.removePreferenceForItem(itemId, uomId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get available UOMs (same category) for an item.
     * e.g. GET /count-uom-preferences/items/100/available-uoms
     */
    @GetMapping("/items/{itemId}/available-uoms")
    public ResponseEntity<List<UomResponseDTO>> getAvailableUomsForItem(@PathVariable Long itemId) {
        // We'll return a simple list of UOM "name (abbreviation)" or something
        List<UnitOfMeasure> uoms = preferenceService.getAvailableUomsForItem(itemId);
        List<UomResponseDTO> dtos = uoms.stream()
                .map(this::toUomDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    // ------------------------------------------
    // 2) SUBRECIPE endpoints
    // ------------------------------------------

    /**
     * List all preferences for a SubRecipe.
     */
    @GetMapping("/subrecipes/{subRecipeId}")
    public ResponseEntity<List<CountUomPreferenceDTO>> getPreferencesForSubRecipe(@PathVariable Long subRecipeId) {
        List<CountUomPreference> prefs = preferenceService.getPreferencesForSubRecipe(subRecipeId);
        List<CountUomPreferenceDTO> dtos = prefs.stream()
                .map(preferenceMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Add or update a preference for a SubRecipe.
     * e.g. POST /count-uom-preferences/subrecipes/200?uomId=5&defaultUom=true
     */
    @PostMapping("/subrecipes/{subRecipeId}")
    public ResponseEntity<CountUomPreferenceDTO> addPreferenceForSubRecipe(
            @PathVariable Long subRecipeId,
            @RequestParam Long uomId,
            @RequestParam(defaultValue = "false") boolean defaultUom
    ) {
        CountUomPreference pref = preferenceService.addPreferenceForSubRecipe(subRecipeId, uomId, defaultUom);
        return ResponseEntity.ok(preferenceMapper.toDto(pref));
    }

    /**
     * Remove a preference for a SubRecipe.
     * e.g. DELETE /count-uom-preferences/subrecipes/200?uomId=5
     */
    @DeleteMapping("/subrecipes/{subRecipeId}")
    public ResponseEntity<Void> removePreferenceForSubRecipe(
            @PathVariable Long subRecipeId,
            @RequestParam Long uomId
    ) {
        preferenceService.removePreferenceForSubRecipe(subRecipeId, uomId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get available UOMs for a subRecipe (matching the subRecipe's base UOM category).
     * e.g. GET /count-uom-preferences/subrecipes/200/available-uoms
     */
    @GetMapping("/subrecipes/{subRecipeId}/available-uoms")
    public ResponseEntity<List<String>> getAvailableUomsForSubRecipe(@PathVariable Long subRecipeId) {
        List<UnitOfMeasure> uoms = preferenceService.getAvailableUomsForSubRecipe(subRecipeId);
        List<String> result = uoms.stream()
                .map(u -> u.getName() + " (" + u.getAbbreviation() + ")")
                .toList();

        return ResponseEntity.ok(result);
    }

    private UomResponseDTO toUomDto(UnitOfMeasure uom) {
        UomResponseDTO dto = new UomResponseDTO();
        dto.setId(uom.getId());
        dto.setName(uom.getName());
        dto.setAbbreviation(uom.getAbbreviation());
        if (uom.getCategory() != null) {
            dto.setCategoryId(uom.getCategory().getId());
            dto.setCategoryName(uom.getCategory().getName());
        }
        return dto;
    }

}
