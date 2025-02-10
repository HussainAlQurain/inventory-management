package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
@Entity
public class InventoryItem {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    /** Basic Identifiers */
    @Column(nullable = false)
    private String name;

    private String sku;  // e.g. internal SKU

    @Column(nullable = false, unique = true)
    private String productCode; // Unique code for the product

    private String description;

    /** Price (initially set manually), eventually auto‐updated as average cost. */
    private Double currentPrice;

    /** Nutritional info, if desired */
    private Double calories;

    /** Many items belong to one company */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** Optional: Category link—one item, one category */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category; // Optional: Category of the inventory item

    /**
     * Base “inventory unit of measure” for counting.
     * e.g. “EA” for each, “KG” for kilograms, etc.
     **/
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_uom_id")
    private UnitOfMeasure inventoryUom;

    @OneToMany(mappedBy = "inventoryItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PurchaseOption> purchaseOptions = new HashSet<>();



    /**
     * If you still want to track which assortments this item belongs to,
     * avoid a direct ManyToMany. Instead, create a bridging entity
     * ‘AssortmentInventoryItem’ with a many‐to‐one to InventoryItem and
     * a many‐to‐one to Assortment.
     */

    /**
     * Optional: Store your sub‐recipe relationships separately, e.g.:
     *  @OneToMany(mappedBy="inventoryItem", cascade=ALL)
     *  private Set<SubRecipeItem> subRecipeItems;
     * or keep them in a dedicated `SubRecipe` entity.
     * In either case, your bridging tables would reference InventoryItem
     * or SubRecipe, not a direct many‐to‐many.
     */

    // Get rid of direct many‐to‐many with Allergen; use bridging if needed.
    // ...
}
