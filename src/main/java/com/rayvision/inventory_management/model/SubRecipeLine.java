package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubRecipeLine {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sub_recipe_line_id_seq")
    @SequenceGenerator(name = "sub_recipe_line_id_seq", sequenceName = "sub_recipe_line_id_seq", allocationSize = 1)
    private Long id;

    /**
     * The parent subRecipe that *this* line belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_sub_recipe_id", nullable = false)
    private SubRecipe parentSubRecipe;

    /**
     * Optionally reference an InventoryItem.
     * If this is non-null, we’re dealing with a “raw ingredient.”
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = true)
    private InventoryItem inventoryItem;

    /**
     * Optionally reference a *child* SubRecipe.
     * If this is non-null, we’re dealing with a sub-recipe as the “ingredient.”
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_sub_recipe_id", nullable = true)
    private SubRecipe childSubRecipe;

    /**
     * The net quantity of this line in the given UOM (below).
     */
    @Column(nullable = false)
    private Double quantity = 0.0;

    /**
     * Wastage % for this line
     * (0.05 for 5%, for example).
     */
    private Double wastagePercent;

    /**
     * The unit of measure for *this line’s quantity*,
     * which can differ from the parent’s yield UOM or from the child SubRecipe’s yield UOM.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_of_measure_id", nullable = false)
    private UnitOfMeasure unitOfMeasure;

    /**
     * (Optional) store a last-computed lineCost if you want to show the cost breakdown per line.
     */
    private Double lineCost;

}
