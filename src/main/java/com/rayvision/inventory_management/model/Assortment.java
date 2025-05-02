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
@Table(indexes = {
    @Index(name = "idx_assortment_company", columnList = "company_id")
})
public class Assortment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assortment_id_seq")
    @SequenceGenerator(name = "assortment_id_seq", sequenceName = "assortment_id_seq", allocationSize = 50)
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToMany
    @JoinTable(
            name = "assortment_inventory_item",
            joinColumns = @JoinColumn(name = "assortment_id"),
            inverseJoinColumns = @JoinColumn(name = "inventory_item_id"),
            indexes = {
                @Index(name = "idx_assortment_items_assortment", columnList = "assortment_id"),
                @Index(name = "idx_assortment_items_item", columnList = "inventory_item_id")
            }
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
