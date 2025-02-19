package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
@Entity
public class MenuItemSubRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The parent menu item
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    /**
     * Which subRecipe or PREPARATION is used in the menu item
     * (e.g., “Marinade,” “Prepared Onion Mix,” etc.).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_recipe_id", nullable = false)
    private SubRecipe subRecipe;

    /**
     * The UOM for how you add this subRecipe to the menu item,
     * e.g. “oz,” “grams,” “cups,” etc.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_of_measure_id", nullable = false)
    private UnitOfMeasure unitOfMeasure;

    /**
     * The quantity of this subRecipe in the above UOM needed for one serving.
     */
    @Column(nullable = false)
    private Double quantity;

    /**
     * Wastage % if you have any shrink or yield loss in the subRecipe portion for this menu item.
     */
    private Double wastagePercent;

    /**
     * Optional stored cost or purely computed from subRecipe cost * quantity etc.
     */
    private Double cost;

}
