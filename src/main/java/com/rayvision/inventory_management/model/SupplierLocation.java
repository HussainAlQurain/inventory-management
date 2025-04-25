package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
public class SupplierLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "supplier_location_id_seq")
    @SequenceGenerator(name = "supplier_location_id_seq", sequenceName = "supplier_location_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

}
