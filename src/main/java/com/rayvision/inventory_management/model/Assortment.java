package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@Setter
@Getter
@NoArgsConstructor
@Builder
@Entity
public class Assortment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    // Many-to-Many relationship with Location (through AssortmentLocation)
    @OneToMany(mappedBy = "assortment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AssortmentLocation> assortmentLocations = new HashSet<>();

    // Many-to-Many relationship with SubRecipe
    @ManyToMany
    @JoinTable(
            name = "assortment_sub_recipes",
            joinColumns = @JoinColumn(name = "assortment_id"),
            inverseJoinColumns = @JoinColumn(name = "sub_recipe_id")
    )
    private Set<SubRecipe> subRecipes = new HashSet<>();


    @ManyToMany
    @JoinTable(
            name = "assortment_purchase_option",
            joinColumns = @JoinColumn(name = "assortment_id"),
            inverseJoinColumns = @JoinColumn(name = "purchase_option_id")
    )
    private Set<PurchaseOption> purchaseOptions = new HashSet<>();
}
