package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Setter
@Getter
@NoArgsConstructor
@Builder
@Entity
public class StockTransaction {


    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;
    private Double quantityChange;   // positive for increases, negative for decreases
    private Double costChange;
    private String transactionType;  // e.g. PURCHASE, TRANSFER_IN, TRANSFER_OUT, USAGE, WASTAGE, ADJUSTMENT

    @ManyToOne
    private Location location;

    @ManyToOne
    private InventoryItem item;

    /**
     * A generic reference ID you can use to link back to
     * any "source" record (like an Order, Transfer, or InventoryCountSession).
     * For example, if it's from a count session, store the sessionId.
     * If from an order, store orderId, etc.
     */
    private Long sourceReferenceId;

}
