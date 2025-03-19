package com.rayvision.inventory_management.model.dto;

import lombok.Data;

@Data
public class LocationDTO {
    private Long id;
    private String name;
    private String code;
    private String address;
    private String city;
    private String state;
    private String zip;
    private String phone;
    private Long companyId;
}
