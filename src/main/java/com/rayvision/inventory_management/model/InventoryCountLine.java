package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class InventoryCountLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which session/header does this line belong to
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "count_session_id")
    private InventoryCountSession countSession;

    // EITHER item or subRecipe (one can be null, the other not).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;

    // NEW: subRecipe for counting preps in the same session
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_recipe_id")
    private SubRecipe subRecipe;      // can be null if counting an item

    // The user can optionally select which “storage area” (fridge, freezer, etc.)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_area_id")
    private StorageArea storageArea;

    private Double countedQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "count_uom_id")
    private UnitOfMeasure countUom;

    private Double convertedQuantityInBaseUom;
    private Double lineTotalValue;

}
