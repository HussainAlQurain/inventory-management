package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
@Table(indexes = {
    @Index(name = "idx_assortment_location_location", columnList = "location_id"),
    @Index(name = "idx_assortment_location_assortment", columnList = "assortment_id")
})
public class AssortmentLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assortment_location_id_seq")
    @SequenceGenerator(name = "assortment_location_id_seq", sequenceName = "assortment_location_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne
    private Location location;

    @ManyToOne
    private Assortment assortment;
}
