package com.rayvision.inventory_management.model;

import com.rayvision.inventory_management.enums.UoMCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
@Entity
public class UnitOfMeasure {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., Each, Box, Kg

    @Column(nullable = false, unique = true)
    private String abbreviation; // e.g., EA, BOX, KG

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UoMCategory category; // COUNT, MASS, VOLUME

    @Column(nullable = false)
    private Double conversionFactor; // e.g., 1 Box = 12 Each => conversionFactor = 12.0
}
