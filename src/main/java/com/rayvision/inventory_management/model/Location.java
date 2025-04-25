package com.rayvision.inventory_management.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
@Entity
public class Location {
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "location_id_seq")
    @SequenceGenerator(name = "location_id_seq", sequenceName = "location_id_seq", allocationSize = 50)
    @Id
    private Long id;

    private String name;

    private String code;

    private String address;

    private String city;

    private String state;

    private String zip;

    private String phone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", nullable = false)
    @JsonBackReference("company-location")
    private Company company;

    @OneToMany(mappedBy = "location")
    private Set<SupplierLocation> supplierLocations = new HashSet<>();

    @OneToMany(mappedBy = "location")
    @JsonManagedReference("location-locationuser")
    private Set<LocationUser> locationUsers = new HashSet<>();

    @OneToMany(mappedBy = "location")
    private Set<AssortmentLocation> assortmentLocations = new HashSet<>();
}
