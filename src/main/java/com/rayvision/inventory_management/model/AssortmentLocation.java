package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
public class AssortmentLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assortment_location_id_seq")
    @SequenceGenerator(name = "assortment_location_id_seq", sequenceName = "assortment_location_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    private Location location;

    @ManyToOne
    private Assortment assortment;
}
