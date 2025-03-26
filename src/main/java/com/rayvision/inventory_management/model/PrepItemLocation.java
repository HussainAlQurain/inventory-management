package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"sub_recipe_id", "location_id"})
})
public class PrepItemLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which sub‐recipe is being physically tracked
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_recipe_id", nullable = false)
    private SubRecipe subRecipe;

    // Which location
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    private Double onHand;
    private Double minOnHand;
    private Double par;
    private Double lastCount;

    private LocalDate lastCountDate; // Add this to match InventoryItemLocation

    @Version
    private Long version; // prevent race conditions
}
