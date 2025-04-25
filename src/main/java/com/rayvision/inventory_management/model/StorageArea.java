package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class StorageArea {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "storage_area_id_seq")
    @SequenceGenerator(name = "storage_area_id_seq", sequenceName = "storage_area_id_seq", allocationSize = 1)
    private Long id;
    private String name;
    private String description;
    @ManyToOne(fetch = FetchType.LAZY)
    private Location location;
}
