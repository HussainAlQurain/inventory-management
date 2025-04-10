package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
@Entity
public class Supplier {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;
    //@Column(unique = true) need to ensure unique supplier name to differentiate between them.
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
    private Set<SupplierEmail> orderEmails = new HashSet<>();

    // Order Phones
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SupplierPhone> orderPhones = new HashSet<>();

    // Authorized Buyers (Locations)
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SupplierLocation> authorizedBuyers = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Transient
    private List<Long> authorizedBuyerIds = new ArrayList<>();

}
