package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SaleLine {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sale_line_id_seq")
    @SequenceGenerator(name = "sale_line_id_seq", sequenceName = "sale_line_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="sale_id", nullable=false)
    private Sale sale;

    // The MenuItem that was sold
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="menu_item_id")
    private MenuItem menuItem;

    // The POS code or name, if you want to store it again
    private String posCode;

    // How many units sold
    private Double quantity;

    // The price the user paid *per unit* at that time
    private Double unitPriceAtSale;

    // Extended revenue = quantity * unitPriceAtSale
    private Double extendedPrice;

    // The cost for THIS line, at the time of sale
    private Double costAtSaleTime;

    // If you want a cost percentage or profit for the line
    private Double profitAtSaleTime; // extendedPrice - costAtSaleTime

}
