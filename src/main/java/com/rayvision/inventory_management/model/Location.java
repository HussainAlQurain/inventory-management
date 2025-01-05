package com.rayvision.inventory_management.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private String name;

    private String code;

    private String address;

    private String city;

    private String state;

    private String zip;

    private String phone;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    @JsonBackReference("company-location")
    private Company company;

    @OneToMany(mappedBy = "location")
    private Set<SupplierLocation> supplierLocations = new HashSet<>();

    @ManyToMany(mappedBy = "locations")
    @JsonIgnore
    private Set<Users> users = new HashSet<>();

    @ManyToMany(mappedBy = "authorizedBuyers")
    @JsonIgnore
    private Set<Assortment> assortments = new HashSet<>();
}
