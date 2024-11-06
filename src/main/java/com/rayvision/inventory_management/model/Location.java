package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
@Entity
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "location_id_seq")
    private Long id;

    private String name;
    private String code;
    private String address;
    private String city;
    private String state;
    private String zip;
    private String phone;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToMany(mappedBy = "locations")
    private Set<Users> users;

    @ManyToMany(mappedBy = "locations")
    private Set<Assortment> assortments;
}
