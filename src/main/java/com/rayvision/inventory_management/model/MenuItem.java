package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@AllArgsConstructor
@Setter
@Getter
@NoArgsConstructor
@Builder
@Entity
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String modifierGroups;
    // could be separate entity or JSON if needed

    // Remove or keep location if you want location-limited menu items:
    // @ManyToOne(fetch = FetchType.LAZY)
    // private Location buyerLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "pos_code", nullable = false, unique = true)
    private String posCode;

    // cost, foodCostPercentage, or other fields.
    private Double cost;

    // Possibly compute cost from sub‐recipes & inventory items:

    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MenuItemInventoryItem> menuItemInventoryItems;

    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MenuItemSubRecipe> menuItemSubRecipes;
}
