package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class SupplierResponseDTO {
    private Long id;
    private String name;
    private String customerNumber;
    private Double minimumOrder;
    private String taxId;
    private Double taxRate;
    private String paymentTerms;
    private String comments;
    private String address;
    private String city;
    private String state;
    private String zip;
    private String ccEmails;

    // The default category (if you want to display it)
    private CategoryResponseDTO defaultCategory;

    // The orderEmails, orderPhones, authorizedBuyers
    private List<SupplierEmailResponseDTO> orderEmails;
    private List<SupplierPhoneResponseDTO> orderPhones;

    // This is optional. If you want to list each location ID or detail:
    private List<SupplierLocationResponseDTO> authorizedBuyers;
}
