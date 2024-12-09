package com.rayvision.inventory_management.model;

import com.rayvision.inventory_management.enums.SubRecipeType;
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
public class SubRecipe {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubRecipeType type; // PREPARATION or SUB_RECIPE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_location_id", nullable = false)
    private Location buyerLocation; // Location (buyer) associated with this sub-recipe

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category; // Category of the sub-recipe

    private Double cost; // Cost in SR

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_of_measure_id", nullable = false)
    private UnitOfMeasure uom; // Unit of Measure for the sub-recipe

    @Column(name = "min_on_hand")
    private Double minOnHand;

    @Column
    private Double par;

    @Column(name = "last_count")
    private Double lastCount;

    @Column(name = "on_hand")
    private Double onHand;

    // Assortments
    @ManyToMany
    @JoinTable(
            name = "sub_recipe_assortments",
            joinColumns = @JoinColumn(name = "sub_recipe_id"),
            inverseJoinColumns = @JoinColumn(name = "assortment_id")
    )
    private Set<Assortment> assortments;

    // SubRecipeItems
    @OneToMany(mappedBy = "subRecipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SubRecipeItem> subRecipeItems;

    // MenuItems
    @ManyToMany(mappedBy = "subRecipes")
    private Set<MenuItem> menuItems;
}
