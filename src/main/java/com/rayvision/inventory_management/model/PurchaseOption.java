package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(indexes = {
    @Index(name = "idx_purchase_option_supplier_enabled", columnList = "supplier_id, orderingEnabled"),
    @Index(name = "idx_purchase_option_inventory_item", columnList = "inventory_item_id")
})
public class PurchaseOption {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "purchase_option_id_seq")
    @SequenceGenerator(name = "purchase_option_id_seq", sequenceName = "purchase_option_id_seq", allocationSize = 1)
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
     * Price history records.
     */
    @OneToMany(mappedBy = "purchaseOption", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PurchaseOptionPriceHistory> priceHistories = new HashSet<>();


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
