package com.rayvision.inventory_management.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@Data
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
    private Company company;

    @ManyToMany(mappedBy = "authorizedBuyers")
    private Set<Supplier> authorizedSuppliers = new HashSet<>();

    @ManyToMany(mappedBy = "locations")
    private Set<Users> users = new HashSet<>();

    @ManyToMany(mappedBy = "authorizedBuyers")
    private Set<Assortment> assortments = new HashSet<>();
}
