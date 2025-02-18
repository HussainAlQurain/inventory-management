package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.Supplier;
import com.rayvision.inventory_management.model.SupplierEmail;
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
@Mapper(componentModel = "spring")
public interface SupplierMapper {

    // 1) Convert from create DTO -> entity
    //    We'll ignore certain fields you plan to set in the Service
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "defaultCategory", ignore = true)    // fetched from DB or set in service
    @Mapping(target = "authorizedBuyers", ignore = true)  // if you have bridging logic in service
    // <-- Add this line so that `dto.authorizedBuyerIds` goes into the transient supplier field
    @Mapping(source = "authorizedBuyerIds", target = "authorizedBuyerIds")
    Supplier fromSupplierCreateDTO(SupplierCreateDTO dto);

    // 2) Convert entity -> response
    //    We'll map sub-entities to their response automatically
    @Mapping(source = "orderEmails", target = "orderEmails")
    @Mapping(source = "orderPhones", target = "orderPhones")
    @Mapping(source = "defaultCategory", target = "defaultCategory")  // This gives CategoryResponseDTO
    @Mapping(source = "authorizedBuyers", target = "authorizedBuyers")
    SupplierResponseDTO toSupplierResponseDTO(Supplier supplier);

    // 3) Sub-entity mappings
    //    Each phone/email is in the createDTO as SupplierPhoneDTO / SupplierEmailDTO
    List<SupplierEmail> mapEmailDTOs(List<SupplierEmailDTO> dtos);
    List<SupplierPhone> mapPhoneDTOs(List<SupplierPhoneDTO> dtos);

    // 4) Sub-entity -> response (already do so inside 'toSupplierResponseDTO',
    //    but if we need them separately:
    SupplierEmailResponseDTO toSupplierEmailResponseDTO(SupplierEmail email);
    SupplierPhoneResponseDTO toSupplierPhoneResponseDTO(SupplierPhone phone);

    // 5) After mapping from createDTO -> Supplier, we must link each phone/email back to the supplier
    @AfterMapping
    default void linkEmailsAndPhones(
            @MappingTarget Supplier supplier,
            SupplierCreateDTO dto
    ) {
        // The generation of orderEmails / orderPhones will be handled automatically
        // by mapEmailDTOs() / mapPhoneDTOs(), but we still need to do:
        // 'email.setSupplier(supplier)' so JPA knows the relationship.

        if (supplier.getOrderEmails() != null) {
            supplier.getOrderEmails().forEach(e -> e.setSupplier(supplier));
        }
        if (supplier.getOrderPhones() != null) {
            supplier.getOrderPhones().forEach(p -> p.setSupplier(supplier));
        }
    }
}
