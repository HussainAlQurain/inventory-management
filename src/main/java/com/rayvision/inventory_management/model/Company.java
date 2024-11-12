package com.rayvision.inventory_management.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
@Entity
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "company_id_seq")
    private Long id;
    private String name;
    private String tax_id;
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
}
