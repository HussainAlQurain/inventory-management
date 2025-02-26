package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class InventoryCountSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id")
    private Location location; // The place where we’re counting

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDate countDate;

    // Could be an enum or just a String: "START_OF_DAY" or "END_OF_DAY"
    private String dayPart;

    private String description;

    private boolean locked;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDate lockedDate; // or lockedAt (DateTime) if you want a timestamp

    // One session has many lines
    @OneToMany(mappedBy = "countSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<InventoryCountLine> lines = new HashSet<>();

}
