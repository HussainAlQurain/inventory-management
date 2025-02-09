package com.rayvision.inventory_management.model;

import com.rayvision.inventory_management.enums.SubRecipeType;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@AllArgsConstructor
@Setter
@Getter
@NoArgsConstructor
@Builder
@Entity
public class SubRecipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g., “Salsa Base,” “Marinade,” “Donut Dough,” etc.
    @Column(nullable = false)
    private String name;

    /**
     * PREPARATION or SUB_RECIPE, as you had before.
     * - SUB_RECIPE: purely a recipe formula, no on-hand tracking.
     * - PREPARATION: physically produced item, we want to do inventory counts.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubRecipeType type;

    // Category is optional. If it’s purely logical, you can keep it.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * "cost" can be computed from subRecipeItems,
     * but you can store it here if you want a quick total reference
     */
    private Double cost;

    /**
     * A single “master” UOM for the total yield, e.g. "kg" or "liters" or “batches.”
     * This is not the measure of each ingredient, but rather how you measure
     * the final sub-recipe.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_of_measure_id", nullable = false)
    private UnitOfMeasure uom;

    /**
     * If 'type == PREPARATION', you may want to track
     * inventory levels for this “recipe.”
     * Instead of storing them directly here,
     * consider a bridging entity so you can track them by location.
     */

    // e.g. Instead of min_on_hand, par, on_hand directly here,
    // create a bridging table "PrepItemLocation" if needed.

    // Keep the subRecipeItems referencing InventoryItems:
    @OneToMany(mappedBy = "subRecipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SubRecipeItem> subRecipeItems;

    // If you want to allow subRecipe to appear in multiple assortments
    // without direct ManyToMany, you could do bridging (AssortmentSubRecipe).

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

}
