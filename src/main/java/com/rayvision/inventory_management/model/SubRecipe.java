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

    /**
     * e.g. “Salsa Base,” “Marinade,” “Donut Dough,” etc.
     */
    @Column(nullable = false)
    private String name;

    /**
     * PREPARATION or SUB_RECIPE
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubRecipeType type;

    /**
     * Category is optional.
     * If subRecipe belongs to a certain logical category: “Sauces,” “Doughs,” “Dressings,” etc.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * The cost can be computed from subRecipeItems,
     * but you can store a last-computed cost here for quick reference.
     */
    private Double cost;

    /**
     * The UOM for the final yield of this subRecipe,
     * e.g. “kg,” “liters,” “batches,” “EA,” etc.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_of_measure_id", nullable = false)
    private UnitOfMeasure uom;

    /**
     * The quantity of the final yield, e.g. “10.0” if the yield is 10 lb or 10 liters, etc.
     */
    private Double yieldQty;

    /**
     * Photo or image URL for the subRecipe, if desired.
     */
    private String photoUrl;

    /**
     * Possibly store times in minutes.
     */
    private Integer prepTimeMinutes;  // how long to prep
    private Integer cookTimeMinutes;  // how long to cook
    // total or “ready in” could be computed from those.

    /**
     * Optional textual instructions or notes for making it.
     */
    @Lob
    private String instructions;

    /**
     * The subRecipeItems referencing InventoryItems that form this recipe.
     */
    @OneToMany(mappedBy = "subRecipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SubRecipeItem> subRecipeItems;

    /**
     * For (type == PREPARATION),
     * you can track location-based inventory in a bridging table (e.g. `PrepItemLocation`).
     */

    /**
     * Many subRecipes belong to one company.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

}
