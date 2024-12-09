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
public class Supplier {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;
    private String name;

    private String customerNumber;

    private Double minimumOrder;

    private String taxId;

    private Double taxRate;

    private String paymentTerms;

    private String comments;

    // Address Fields
    private String address;

    private String city;

    private String state;

    private String zip;

    // CC Emails (comma-separated)
    @Column(name = "cc_emails")
    private String ccEmails;

    // Default Category
    @ManyToOne
    @JoinColumn(name = "default_category_id")
    private Category defaultCategory;

    // Order Emails
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SupplierEmail> orderEmails;

    // Order Phones
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SupplierPhone> orderPhones;

    // Authorized Buyers (Locations)
    @ManyToMany
    @JoinTable(
            name = "supplier_authorized_buyers",
            joinColumns = @JoinColumn(name = "supplier_id"),
            inverseJoinColumns = @JoinColumn(name = "location_id")
    )
    private Set<Location> authorizedBuyers;

}
