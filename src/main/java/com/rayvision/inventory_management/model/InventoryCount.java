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
@Builder
@Entity
public class InventoryCount {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_count_id_seq")
    @SequenceGenerator(name = "inventory_count_id_seq", sequenceName = "inventory_count_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    private LocalDate countDate;

    private Double quantity;
}
