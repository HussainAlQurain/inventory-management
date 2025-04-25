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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stock_transaction_id_seq")
    @SequenceGenerator(name = "stock_transaction_id_seq", sequenceName = "stock_transaction_id_seq", allocationSize = 1)
    private Long id;

    private LocalDate date;
    private Double quantityChange;   // positive for increases, negative for decreases
    private Double costChange;
    private String transactionType;  // e.g. PURCHASE, TRANSFER_IN, TRANSFER_OUT, USAGE, WASTAGE, ADJUSTMENT

    @ManyToOne
    private Location location;

    @ManyToOne
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_recipe_id")
    private SubRecipe subRecipe;  // optionally null


    /**
     * A generic reference ID you can use to link back to
     * any "source" record (like an Order, Transfer, or InventoryCountSession).
     * For example, if it's from a count session, store the sessionId.
     * If from an order, store orderId, etc.
     */
    private Long sourceReferenceId;

}
