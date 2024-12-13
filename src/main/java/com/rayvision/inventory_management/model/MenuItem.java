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
public class MenuItem {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "modifier_groups")
    private String modifierGroups; // Can be a separate entity if needed

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_location_id", nullable = false)
    private Location buyerLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "pos_code", nullable = false, unique = true)
    private String posCode;

    @Column(nullable = false)
    private Double cost;

    @Column(name = "food_cost_percentage", nullable = false)
    private Double foodCostPercentage; // e.g., 17.63%

    @Column(name = "retail_price_excluding_tax", nullable = false)
    private Double retailPriceExcludingTax;

    // MenuItemInventoryItems
    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MenuItemInventoryItem> menuItemInventoryItems;

    // MenuItemSubRecipes
    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MenuItemSubRecipe> menuItemSubRecipes;

}
