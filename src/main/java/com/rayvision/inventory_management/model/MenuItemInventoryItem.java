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
public class MenuItemInventoryItem {
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
     * The raw inventory item used in this menu item
     * (e.g., “Beef Patty,” “Cheese Slice,” “Lettuce”).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    /**
     * The unit of measure for how you measure this ingredient in the recipe,
     * e.g. “EA,” “oz,” “grams,” etc. Might differ from item’s base UOM.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_of_measure_id", nullable = false)
    private UnitOfMeasure unitOfMeasure;

    /**
     * The quantity of the ingredient in the above UOM
     * needed for one serving of the menu item.
     */
    @Column(nullable = false)
    private Double quantity;

    /**
     * Wastage % for this ingredient specifically, if any.
     * e.g. 0.05 for 5% wastage
     */
    private Double wastagePercent;

    /**
     * Potentially store cost if you want a snapshot, or compute on the fly
     * (inventoryItem.currentPrice * quantity * (1 + wastage)).
     */
    private Double cost;
}
