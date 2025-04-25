package com.rayvision.inventory_management.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
public class LocationUser {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "location_user_id_seq")
    @SequenceGenerator(name = "location_user_id_seq", sequenceName = "location_user_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne
    @JsonBackReference("location-locationuser")
    private Location location;

    @ManyToOne
    @JsonBackReference("user-locationuser")
    private Users user;
}
