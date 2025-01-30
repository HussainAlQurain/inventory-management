package com.rayvision.inventory_management.model;

import com.rayvision.inventory_management.enums.SubRecipeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
@Entity
public class SubRecipeItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_recipe_id", nullable = false)
    private SubRecipe subRecipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_of_measure_id", nullable = false)
    private UnitOfMeasure unitOfMeasure;

    // e.g. 2.0 “kg”, 5.0 “liters” of the ingredient
    @Column(nullable = false)
    private Double quantity;
}
