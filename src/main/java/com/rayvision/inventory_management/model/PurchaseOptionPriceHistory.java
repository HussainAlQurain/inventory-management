package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
public class PurchaseOptionPriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Price recorded at the time of update.
     */
    private Double price;

    /**
     * When this price was recorded.
     */
    private LocalDateTime timestamp;

    /**
     * Link back to the PurchaseOption.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_option_id", nullable = false)
    private PurchaseOption purchaseOption;

}
