package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.mappers.impl.LocationIdMapper;
import com.rayvision.inventory_management.model.Supplier;
import com.rayvision.inventory_management.model.SupplierEmail;
import com.rayvision.inventory_management.model.SupplierLocation;
import com.rayvision.inventory_management.model.SupplierPhone;
import com.rayvision.inventory_management.model.dto.*;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * Dedicated mapper for Supplier and its sub-entities (phones/emails).
 */
@Mapper(componentModel = "spring", uses = {LocationIdMapper.class})
public interface SupplierMapper {

    // 1) Convert from create DTO -> entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "defaultCategory", ignore = true)
    @Mapping(target = "authorizedBuyers", ignore = true)
    @Mapping(source = "authorizedBuyerIds", target = "authorizedBuyerIds")
    Supplier fromSupplierCreateDTO(SupplierCreateDTO dto);

    // 2) Convert entity -> response DTO
    @Mapping(source = "orderEmails", target = "orderEmails")
    @Mapping(source = "orderPhones", target = "orderPhones")
    @Mapping(source = "defaultCategory", target = "defaultCategory")
    @Mapping(source = "authorizedBuyers", target = "authorizedBuyers")
    SupplierResponseDTO toSupplierResponseDTO(Supplier supplier);

    // 3) Sub-entity list mappers
    List<SupplierEmail> mapEmailDTOs(List<SupplierEmailDTO> dtos);

    List<SupplierPhone> mapPhoneDTOs(List<SupplierPhoneDTO> dtos);

    // 4) Individual email mappings
    @Mapping(source = "locationId", target = "location")
    @Mapping(target = "supplier", ignore = true)
    SupplierEmail toEntity(SupplierEmailDTO dto);

    @Mapping(source = "location", target = "locationId")
    SupplierEmailDTO toDTO(SupplierEmail entity);

    // 5) Individual phone mappings
    @Mapping(source = "locationId", target = "location")
    @Mapping(target = "supplier", ignore = true)
    SupplierPhone toEntity(SupplierPhoneDTO dto);

    @Mapping(source = "location", target = "locationId")
    SupplierPhoneDTO toDTO(SupplierPhone entity);

    // 6) Email/phone -> response
    @Mapping(source = "location.id", target = "locationId")
    SupplierEmailResponseDTO toSupplierEmailResponseDTO(SupplierEmail email);

    @Mapping(source = "location.id", target = "locationId")
    SupplierPhoneResponseDTO toSupplierPhoneResponseDTO(SupplierPhone phone);

    List<SupplierEmailResponseDTO> toEmailResponseDTOs(List<SupplierEmail> emails);

    List<SupplierPhoneResponseDTO> toPhoneResponseDTOs(List<SupplierPhone> phones);

    // 7) Authorized buyer mapping (if needed)
    @Mapping(source = "location.id", target = "locationId")
    SupplierLocationResponseDTO toDTO(SupplierLocation entity);

    List<SupplierLocationResponseDTO> toDTOs(List<SupplierLocation> entities);

    // 8) After mapping logic to set back-references
    @AfterMapping
    default void linkEmailsAndPhones(
            @MappingTarget Supplier supplier,
            SupplierCreateDTO dto
    ) {
        if (supplier.getOrderEmails() != null) {
            supplier.getOrderEmails().forEach(e -> e.setSupplier(supplier));
        }
        if (supplier.getOrderPhones() != null) {
            supplier.getOrderPhones().forEach(p -> p.setSupplier(supplier));
        }
    }
}

