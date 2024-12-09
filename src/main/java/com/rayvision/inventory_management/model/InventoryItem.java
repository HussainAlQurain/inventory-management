package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
@Entity
public class InventoryItem {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false, unique = true)
    private String productCode; // Unique code for the product

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category; // Optional: Category of the inventory item

    // Relationships
    @ManyToMany(mappedBy = "inventoryItems")
    private Set<Assortment> assortments;

    @ManyToMany(mappedBy = "inventoryItems")
    private Set<SubRecipe> subRecipes;

    @OneToMany(mappedBy = "inventoryItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SubRecipeItem> subRecipeItems;

    @OneToMany(mappedBy = "inventoryItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MenuItemInventoryItem> menuItemInventoryItems;

    @OneToMany(mappedBy = "inventoryItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InventoryCount> inventoryCounts;

    @ManyToMany(mappedBy = "inventoryItems")
    private Set<MenuItem> menuItems;

    @ManyToMany
    @JoinTable(
            name = "inventory_item_allergens",
            joinColumns = @JoinColumn(name = "inventory_item_id"),
            inverseJoinColumns = @JoinColumn(name = "allergen_id")
    )
    private Set<Allergen> allergens = new HashSet<>();
}
