package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_menu_item_id", nullable = false)
    private MenuItem parentMenuItem;

    // Only one of these three should be non-null per line
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_recipe_id")
    private SubRecipe subRecipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_menu_item_id")
    private MenuItem childMenuItem;

    @Column(nullable = false)
    private Double quantity = 0.0;

    private Double wastagePercent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_of_measure_id", nullable = false)
    private UnitOfMeasure unitOfMeasure;

    private Double lineCost;

}
