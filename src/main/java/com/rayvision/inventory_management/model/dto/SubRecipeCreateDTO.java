package com.rayvision.inventory_management.model.dto;

import com.rayvision.inventory_management.enums.SubRecipeType;
import lombok.Data;

import java.util.List;

/**
 * DTO for creating or updating a SubRecipe
 * including bridging lines to InventoryItems (SubRecipeItem).
 */
@Data
public class SubRecipeCreateDTO {

    private String name;
    private SubRecipeType type;    // e.g. SUB_RECIPE or PREPARATION

    // references an existing Category by ID
    private Long categoryId;

    // references an existing UOM by ID (for the final yield)
    private Long uomId;

    /**
     * Possibly the total yield quantity in that UOM,
     * e.g. 10.0 kg
     */
    private Double yieldQty;

    /**
     * The photo or image link for referencing instructions or UI
     */
    private String photoUrl;

    /**
     * Optional times and instructions
     */
    private Integer prepTimeMinutes;  // e.g. 20
    private Integer cookTimeMinutes;  // e.g. 30
    private String instructions;      // e.g. steps/notes

    /**
     * If you want to store cost or compute from bridging lines,
     * you can either pass it or let the service compute.
     */
    private Double cost;

    /**
     * The bridging lines to InventoryItems (subRecipeItems).
     */
    private List<SubRecipeLineDTO> lines;  // or call it “ingredients” still
}
