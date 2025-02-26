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

    // Which item is being counted
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;

    // The user can optionally select which “storage area” (fridge, freezer, etc.) within that location
    // so we can filter lines later by storageArea.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_area_id")
    private StorageArea storageArea;

    // The quantity in whichever UOM the user chooses to count
    private Double countedQuantity;

    // The UOM that the user used for counting
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "count_uom_id")
    private UnitOfMeasure countUom;

    // If you want to store the item’s “base quantity” for easy calculations
    // e.g. 2 boxes => 2 * boxConversionFactor => 24 each
    private Double convertedQuantityInBaseUom;

    // If you want to store line cost or value:
    private Double lineTotalValue;

}
