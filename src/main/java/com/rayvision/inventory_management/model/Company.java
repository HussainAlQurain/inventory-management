package com.rayvision.inventory_management.model;

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
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String taxId;
    private String phone;
    private String mobile;
    private String email;
    private String state;
    private String city;
    private String address;
    private String zip;

    private Boolean addPurchasedItemsToFavorites;
    private String logo;
    private Double allowedInvoiceDeviation;
    private String accountingSoftware;
    private Boolean exportDeliveryNotesAsBills;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("company-companyuser")
    private Set<CompanyUser> companyUsers = new HashSet<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("company-location")
    private Set<Location> locations = new HashSet<>();

}
