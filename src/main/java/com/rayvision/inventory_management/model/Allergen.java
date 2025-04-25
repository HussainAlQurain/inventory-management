package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
@Entity
public class Allergen {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "allergen_id_seq")
    @SequenceGenerator(name = "allergen_id_seq", sequenceName = "allergen_id_seq", allocationSize = 50)
    private Long id;

    private String name;

    private String description;

//    @ManyToMany(mappedBy = "allergens")
//    private Set<InventoryItem> inventoryItems;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;


}
