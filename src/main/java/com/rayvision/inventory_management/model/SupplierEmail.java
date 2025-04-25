package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
@Entity
public class SupplierEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "supplier_email_id_seq")
    @SequenceGenerator(name = "supplier_email_id_seq", sequenceName = "supplier_email_id_seq", allocationSize = 50)
    private Long id;

    private String email;

    private boolean isDefault;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = true)
    private Location location;

}
