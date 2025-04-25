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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transfer_line_id_seq")
    @SequenceGenerator(name = "transfer_line_id_seq", sequenceName = "transfer_line_id_seq", allocationSize = 1)
    private Long id;

    /* parent */
    @ManyToOne @JoinColumn(name = "transfer_id")
    private Transfer transfer;

    /* EITHER inventoryItem OR subRecipe (prep) -------------------------------- */
    @ManyToOne @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;          // nullable

    @ManyToOne @JoinColumn(name = "sub_recipe_id")
    private SubRecipe subRecipe;                  // nullable – only PREPARATION type

    /* How the user entered the quantity */
    private Double quantity;                      // in the user’s UoM
    @ManyToOne @JoinColumn(name = "unit_of_measure_id")
    private UnitOfMeasure unitOfMeasure;          // never null

    /* optional cost fields – purely informational */
    private Double costPerUnit;
    private Double totalCost;

}
