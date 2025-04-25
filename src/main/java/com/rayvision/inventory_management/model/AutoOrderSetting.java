package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="auto_order_setting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AutoOrderSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auto_order_setting_id_seq")
    @SequenceGenerator(name = "auto_order_setting_id_seq", sequenceName = "auto_order_setting_id_seq", allocationSize = 50)
    private Long id;

    @OneToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="location_id", unique=true, nullable=false)
    private Location location;

    /**
     * If this is true, we do auto-ordering for this location.
     */
    private boolean enabled;

    /**
     * Frequency in seconds, e.g. 300 => runs every 5 min.
     * We'll do a "global scheduler" that runs e.g. every 30s or 60s,
     * then checks if it’s time for each location to do an auto-order check.
     */
    private Integer frequencySeconds;

    /**
     * The last time we actually performed an auto-order check for this location.
     */
    private LocalDateTime lastCheckTime;

    /**
     * The user ID we use as "createdByUserId" in the purchase order.
     * e.g. 99999999 as a system user.
     */
    private Long systemUserId;

    // Possibly a default comment to put in the order, like "Auto-order by system"
    private String autoOrderComment;

    // You might store more fields (like a default supplier if needed)

}
