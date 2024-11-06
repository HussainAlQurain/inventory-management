package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Entity
@Builder
public class InventoryCount {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_count_id_seq")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    private LocalDate countDate;
    private Double quantity;
}
