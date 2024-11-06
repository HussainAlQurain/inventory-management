package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.*;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
@Entity
public class SubRecipe {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sub_recipe_id_seq")
    private Long id;

    private String name;
    private String description;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToMany
    @JoinTable(
            name = "sub_recipe_inventory_item",
            joinColumns = @JoinColumn(name = "sub_recipe_id"),
            inverseJoinColumns = @JoinColumn(name = "inventory_item_id")
    )
    @MapKeyColumn(name = "quantity")
    private Map<InventoryItem, Double> inventoryItems;

    @ManyToMany(mappedBy = "subRecipes")
    private Set<MenuItem> menuItems;

}
