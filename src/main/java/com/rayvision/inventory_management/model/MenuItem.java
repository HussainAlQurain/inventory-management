package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;


/**
 * Represents a final sold menu item (e.g. burger, pizza), possibly composed
 * of both raw InventoryItems and subRecipes (preps).
 */

@AllArgsConstructor
@Setter
@Getter
@NoArgsConstructor
@Builder
@Entity
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "menu_item_id_seq")
    @SequenceGenerator(name = "menu_item_id_seq", sequenceName = "menu_item_id_seq", allocationSize = 50)
    private Long id;

    /**
     * Human-friendly name of the menu item, e.g. “Cheeseburger”
     */
    private String name;

    /**
     * The category (e.g. “Food”, “Beverages,” “Desserts,” etc.).
     * Or it could be an internal menu category for grouping.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * POS code is often how the item is identified in the POS system,
     * e.g. “BRG_CHZ” or “12345”.
     */
    @Column(name = "pos_code", nullable = false, unique = true)
    private String posCode;

    /**
     * The total cost to produce one “serving” or “unit” of this menu item,
     * typically computed from bridging entities.
     * If you want to store a cached cost, keep it here.
     */
    private Double cost;

    /**
     * The retail price (excluding tax) you sell the item for.
     */
    private Double retailPriceExclTax;

    /**
     * If you want to store a direct “food cost %” for quick reference
     * (cost / retailPrice * 100).
     * Alternatively, you can compute it on the fly.
     */
    private Double foodCostPercentage;

    /**
     * If you want an upper limit on acceptable food cost,
     * you can store it here for alerting or reporting. E.g. 30%
     */
    private Double maxAllowedFoodCostPct;

    /**
     * Possibly store other fields like “modifierGroups” or “notes”.
     */
    private String modifierGroups; // or consider separate table/JSON, etc.

    @OneToMany(mappedBy = "parentMenuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MenuItemLine> menuItemLines = new HashSet<>();

    /**
     * If needed, track which company this belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;


}
