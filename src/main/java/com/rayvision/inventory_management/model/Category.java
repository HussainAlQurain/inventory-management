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
public class Category {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., Prep Zafi, Prep Lucas Branch

    private String description;

    // Relationships
    @OneToMany(mappedBy = "defaultCategory")
    private Set<Supplier> suppliers;

    @OneToMany(mappedBy = "category")
    private Set<InventoryItem> inventoryItems;

    @OneToMany(mappedBy = "category")
    private Set<SubRecipe> subRecipes;

    @OneToMany(mappedBy = "category")
    private Set<MenuItem> menuItems; // If MenuItem also has a category

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

}
