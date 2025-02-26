package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@Setter
@Getter
@NoArgsConstructor
@Builder
@Entity
public class TransferLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The parent Transfer
    @ManyToOne
    @JoinColumn(name = "transfer_id")
    private Transfer transfer;

    // The item being transferred
    @ManyToOne
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem item;

    private Double quantity;

    // optional cost if you want to track cost for the line
    private Double costPerUnit;
    private Double totalCost;

}
