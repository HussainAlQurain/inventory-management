package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PurchaseOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Which inventory item does this purchase option belong to?
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    /**
     * If you allow multiple suppliers per item,
     * link the supplier here.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    /**
     * Price for this supplier + ordering unit.
     * e.g. 120.00 SAR per case, or 10.00 SAR per kg.
     */
    private Double price;

    /**
     * Tax rate, if needed separately for each purchase option.
     * If you have a default tax rate, you can store it or override it here.
     */
    private Double taxRate;

    /**
     * The unit in which you purchase from this supplier,
     * which may differ from the item’s base "inventoryUom".
     * e.g. "CASE" vs. the item’s "EA"
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordering_uom_id")
    private UnitOfMeasure orderingUom;

    /**
     * If there's an inner pack quantity or packs per case,
     * you can store them here to handle conversions.
     */
    private Double innerPackQuantity;    // e.g. 12 items in a pack
    private Double packsPerCase;         // e.g. 8 packs per case

    /**
     * Min order quantity for this supplier's packaging
     */
    private Double minOrderQuantity;

    /**
     * True if this is the main or preferred supplier / purchase option
     * for the item’s cost calculations.
     */
    private boolean mainPurchaseOption;

    /**
     * Enable or disable ordering from this supplier or
     * for this packaging arrangement.
     */
    private boolean orderingEnabled;

    /**
     * Misc. fields from your UI / supplier details
     * (supplier product code, nickname, etc.)
     */
    private String supplierProductCode;
    private String nickname;
    private String scanBarcode;

}
