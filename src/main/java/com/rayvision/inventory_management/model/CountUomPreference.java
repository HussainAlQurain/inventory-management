package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
@Entity
public class CountUomPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Either link to an item or a prep (one or both can be null)
    @ManyToOne(fetch = FetchType.LAZY)
    private InventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    private SubRecipe subRecipe;

    // The UOM we want to count with
    @ManyToOne(fetch = FetchType.LAZY)
    private UnitOfMeasure countUom;

    // Possibly store a boolean “defaultCountingUom” or an ordering “sortOrder,” etc.
    private boolean defaultUom;

}
