package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class SubRecipeInventoryItem {
    @EmbeddedId
    private SubRecipeInventoryItemId id;

    @ManyToOne
    @MapsId("subRecipeId")
    @JoinColumn(name = "sub_recipe_id")
    private SubRecipe subRecipe;

    @ManyToOne
    @MapsId("inventoryItemId")
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;

    private Double quantity;
}


@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
class SubRecipeInventoryItemId implements java.io.Serializable {
    private Long subRecipeId;
    private Long inventoryItemId;
}