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
public class MenuItemSubRecipe {
    @EmbeddedId
    private MenuItemSubRecipeId id;

    @ManyToOne
    @MapsId("menuItemId")
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    @ManyToOne
    @MapsId("subRecipeId")
    @JoinColumn(name = "sub_recipe_id")
    private SubRecipe subRecipe;

    private Double quantity;
}

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
class MenuItemSubRecipeId implements java.io.Serializable {
    private Long menuItemId;
    private Long subRecipeId;
}
