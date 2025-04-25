package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UnitOfMeasureCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "uom_category_id_seq")
    @SequenceGenerator(name = "uom_category_id_seq", sequenceName = "uom_category_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g. COUNT, MASS, VOLUME

    private String description;

    // Associate with Company so that each category belongs to a specific company.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

}
