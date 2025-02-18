package com.rayvision.inventory_management.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class SupplierCreateDTO {
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
    // Allow the caller to specify emails and phones.
    // Each of these can be either created on the fly or selected via id (if you implement that logic in your service).
    private List<SupplierEmailDTO> emails;
    private List<SupplierPhoneDTO> phones;

    /**
     * Authorized buyers are the locations permitted to purchase from this supplier.
     * The user can select these from existing locations.
     */
    private List<Long> authorizedBuyerIds;

    // >>> New fields for defaultCategory <<<
    private Long defaultCategoryId;          // references an existing Category by ID
}
