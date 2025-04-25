package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "sale",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"pos_reference", "location_id"})
        }
)
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sale_id_seq")
    @SequenceGenerator(name = "sale_id_seq", sequenceName = "sale_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="location_id", nullable=false)
    private Location location;

    private LocalDateTime saleDateTime;

    // For integration with the POS, e.g. the check/receipt number
    @Column(name="pos_reference")
    private String posReference;

    // Summaries
    private Double totalRevenue; // sum of all lines' extendedPrice
    private Double totalCost;    // sum of all lines' costAtSaleTime
    private Double totalProfit;  // totalRevenue - totalCost

    @OneToMany(mappedBy="sale", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<SaleLine> lines = new ArrayList<>();

}
