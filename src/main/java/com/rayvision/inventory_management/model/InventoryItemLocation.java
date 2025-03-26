package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"inventory_item_id", "location_id"})
})
public class InventoryItemLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    // The “minimum” or reorder threshold for this item at this location
    private Double minOnHand;

    // The “par level” for this item at this location
    private Double parLevel;

    // If you want to store a quick “onHand” snapshot (though you also have StockTransaction):
    private Double onHand;

    // Possibly store lastCount or last date counted:
    private Double lastCount;
    private LocalDate lastCountDate;

    @Version
    private Long version;
}
