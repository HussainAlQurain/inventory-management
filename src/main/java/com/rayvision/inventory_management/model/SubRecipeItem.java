package com.rayvision.inventory_management.model;

import com.rayvision.inventory_management.enums.SubRecipeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;


// This is to be removed and replaced by subrecipeline
@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
@Entity
public class SubRecipeItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sub_recipe_item_id_seq")
    @SequenceGenerator(name = "sub_recipe_item_id_seq", sequenceName = "sub_recipe_item_id_seq", allocationSize = 50)
    private Long id;

    /**
     * The parent subRecipe (or PREPARATION).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_recipe_id", nullable = false)
    private SubRecipe subRecipe;

    /**
     * Which InventoryItem we’re using as an ingredient.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    /**
     * The unit of measure for *this ingredient’s quantity*,
     * which may differ from the subRecipe’s yield UOM or the item’s default UOM.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_of_measure_id", nullable = false)
    private UnitOfMeasure unitOfMeasure;

    /**
     * The quantity needed of this ingredient, in the above `unitOfMeasure`.
     * e.g. 2.0 kg, 5.0 liters, 1.0 pound, etc.
     */
    @Column(nullable = false)
    private Double quantity;

    /**
     * Wastage % for this particular ingredient
     * (if you need to adjust cost or quantity for the subRecipe).
     * e.g. 0.05 for 5% wastage
     */
    private Double wastagePercent;
}
