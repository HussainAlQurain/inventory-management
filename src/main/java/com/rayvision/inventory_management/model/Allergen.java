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
public class Allergen {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "allergen_id_seq")
    private Long id;

    private String name;
    private String description;

    @ManyToMany(mappedBy = "allergens")
    private Set<InventoryItem> inventoryItems;

}
