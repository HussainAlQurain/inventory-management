package com.rayvision.inventory_management.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String unitOfMeasure; // e.g., "10 L", "kg"
    private String productCode;
    private Double currentStock; // Current quantity in stock
    private Double threshold; // Minimum stock level to trigger an order
    private Double price; // Price per unit
}
