package com.rayvision.inventory_management.model;

import com.rayvision.inventory_management.enums.UoMCategory;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
@Entity
public class UnitOfMeasure {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "uom_id_seq")
    @SequenceGenerator(name = "uom_id_seq", sequenceName = "uom_id_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., Each, Box, Kg

    @Column(nullable = false, unique = true)
    private String abbreviation; // e.g., EA, BOX, KG

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_category_id", nullable = false)
    private UnitOfMeasureCategory category;

    @Column(nullable = false)
    private Double conversionFactor; // e.g., 1 Box = 12 Each => conversionFactor = 12.0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;


}
