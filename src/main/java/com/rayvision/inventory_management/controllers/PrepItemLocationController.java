package com.rayvision.inventory_management.controllers;


import com.rayvision.inventory_management.model.PrepItemLocation;
import com.rayvision.inventory_management.service.PrepItemLocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tracks location-based on-hand for a PREPARATION subRecipe
 */

@RestController
@RequestMapping("/prep-locations")
public class PrepItemLocationController {
    private final PrepItemLocationService prepItemLocationService;

    public PrepItemLocationController(PrepItemLocationService prepItemLocationService) {
        this.prepItemLocationService = prepItemLocationService;
    }

    // GET all bridging rows for a subRecipe
    @GetMapping("/sub-recipe/{subRecipeId}")
    public ResponseEntity<List<PrepItemLocation>> getBySubRecipe(@PathVariable Long subRecipeId) {
        List<PrepItemLocation> list = prepItemLocationService.getBySubRecipe(subRecipeId);
        return ResponseEntity.ok(list);
    }

    // GET all bridging rows for a location
    @GetMapping("/location/{locationId}")
    public ResponseEntity<List<PrepItemLocation>> getByLocation(@PathVariable Long locationId) {
        List<PrepItemLocation> list = prepItemLocationService.getByLocation(locationId);
        return ResponseEntity.ok(list);
    }

    // GET single bridging row
    @GetMapping("/{bridgingId}")
    public ResponseEntity<PrepItemLocation> getOne(@PathVariable Long bridgingId) {
        return prepItemLocationService.getOne(bridgingId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // CREATE bridging row
    @PostMapping
    public ResponseEntity<PrepItemLocation> create(@RequestParam Long subRecipeId,
                                                   @RequestParam Long locationId,
                                                   @RequestBody PrepItemLocation prepLoc) {
        // pass subRecipeId, locationId, plus the partial fields in the request body
        PrepItemLocation created = prepItemLocationService.create(subRecipeId, locationId, prepLoc);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // UPDATE bridging row
    @PutMapping("/{bridgingId}")
    public ResponseEntity<PrepItemLocation> update(@PathVariable Long bridgingId,
                                                   @RequestBody PrepItemLocation patch) {
        try {
            PrepItemLocation updated = prepItemLocationService.update(bridgingId, patch);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE bridging row
    @DeleteMapping("/{bridgingId}")
    public ResponseEntity<Void> delete(@PathVariable Long bridgingId) {
        try {
            prepItemLocationService.delete(bridgingId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
