package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
@Entity
public class SupplierPhone {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "supplier_phone_id_seq")
    @SequenceGenerator(name = "supplier_phone_id_seq", sequenceName = "supplier_phone_id_seq", allocationSize = 50)
    private Long id;

    private String phoneNumber;

    private boolean isDefault;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "location_id", nullable = true)
    private Location location;
}
