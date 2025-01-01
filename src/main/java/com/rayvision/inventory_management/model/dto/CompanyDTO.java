package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class CompanyDTO {
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
    private List<UserDTO> users;
}
