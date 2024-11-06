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
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_item_id_seq")
    private Long id;

    private String name;
    private String sku;
    private String description;
    private String unitOfMeasure;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToMany(mappedBy = "inventoryItems")
    private Set<Assortment> assortments;

    @ManyToMany
    @JoinTable(
            name = "inventory_item_allergen",
            joinColumns = @JoinColumn(name = "inventory_item_id"),
            inverseJoinColumns = @JoinColumn(name = "allergen_id")
    )
    private Set<Allergen> allergens;

    @ManyToMany(mappedBy = "inventoryItems")
    private Set<SubRecipe> subRecipes;
}
