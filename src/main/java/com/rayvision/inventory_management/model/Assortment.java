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
public class Assortment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assortment_id_seq")
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToMany
    @JoinTable(
            name = "assortment_inventory_item",
            joinColumns = @JoinColumn(name = "assortment_id"),
            inverseJoinColumns = @JoinColumn(name = "inventory_item_id")
    )
    private Set<InventoryItem> inventoryItems;

    @ManyToMany
    @JoinTable(
            name = "assortment_location",
            joinColumns = @JoinColumn(name = "assortment_id"),
            inverseJoinColumns = @JoinColumn(name = "location_id")
    )
    private Set<Location> locations;
}
