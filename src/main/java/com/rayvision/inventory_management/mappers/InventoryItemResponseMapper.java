package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryItemResponseMapper {
    // 1) The main mapping: InventoryItem -> InventoryItemResponseDTO
    InventoryItemResponseDTO toInventoryItemResponseDTO(InventoryItem entity);

    // 2) Category -> CategoryResponseDTO
    CategoryResponseDTO toCategoryResponseDTO(Category category);

    // 3) PurchaseOption -> PurchaseOptionResponseDTO
    @Mapping(source = "orderingUom", target = "orderingUom") // or let it figure out automatically
    @Mapping(source = "supplier", target = "supplier")
    PurchaseOptionResponseDTO toPurchaseOptionResponseDTO(PurchaseOption po);

    // 4) UnitOfMeasure -> UnitOfMeasureResponseDTO
    UnitOfMeasureResponseDTO toUnitOfMeasureResponseDTO(UnitOfMeasure uom);

    // 5) UnitOfMeasureCategory -> UnitOfMeasureCategoryResponseDTO
    UnitOfMeasureCategoryResponseDTO toUomCategoryResponseDTO(UnitOfMeasureCategory cat);

    // 6) Supplier -> SupplierResponseDTO
    @Mapping(source = "orderEmails", target = "orderEmails")
    @Mapping(source = "orderPhones", target = "orderPhones")
    @Mapping(source = "defaultCategory", target = "defaultCategory")
    @Mapping(source = "authorizedBuyers", target = "authorizedBuyers")
    SupplierResponseDTO toSupplierResponseDTO(Supplier supplier);

    // 7) SupplierEmail -> SupplierEmailResponseDTO
    SupplierEmailResponseDTO toSupplierEmailResponseDTO(SupplierEmail email);

    // 8) SupplierPhone -> SupplierPhoneResponseDTO
    SupplierPhoneResponseDTO toSupplierPhoneResponseDTO(SupplierPhone phone);

    // 9) SupplierLocation -> SupplierLocationResponseDTO
    @Mapping(source = "location.id", target = "locationId")
    SupplierLocationResponseDTO toSupplierLocationResponseDTO(SupplierLocation sl);

}
