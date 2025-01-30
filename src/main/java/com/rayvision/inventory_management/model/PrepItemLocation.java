package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
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

}
