package com.rayvision.inventory_management.model;

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
public class Assortment {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToMany
    @JoinTable(
            name = "assortment_inventory_item",
            joinColumns = @JoinColumn(name = "assortment_id"),
            inverseJoinColumns = @JoinColumn(name = "inventory_item_id")
    )
    private Set<InventoryItem> inventoryItems;

    // Many-to-Many relationship with Location (Authorized Buyers)
    @ManyToMany
    @JoinTable(
            name = "assortment_authorized_buyers",
            joinColumns = @JoinColumn(name = "assortment_id"),
            inverseJoinColumns = @JoinColumn(name = "location_id")
    )
    private Set<Location> authorizedBuyers;

    // Many-to-Many relationship with SubRecipe
    @ManyToMany
    @JoinTable(
            name = "assortment_sub_recipes",
            joinColumns = @JoinColumn(name = "assortment_id"),
            inverseJoinColumns = @JoinColumn(name = "sub_recipe_id")
    )
    private Set<SubRecipe> subRecipes;

}
