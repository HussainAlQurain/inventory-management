package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.Assortment;
import com.rayvision.inventory_management.model.dto.AssortmentDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AssortmentMapper {
    // Map from entity -> DTO
    @Mapping(target = "companyId", source = "company.id")
    @Mapping(target = "itemIds", expression =
            "java( assortment.getInventoryItems() == null ? null : " +
                    "assortment.getInventoryItems().stream()" +
                    ".map(item -> item.getId())" +
                    ".collect(java.util.stream.Collectors.toSet()) )")
    @Mapping(target = "subRecipeIds", expression =
            "java( assortment.getSubRecipes() == null ? null : " +
                    "assortment.getSubRecipes().stream()" +
                    ".map(sr -> sr.getId())" +
                    ".collect(java.util.stream.Collectors.toSet()) )")
    @Mapping(target = "purchaseOptionIds", expression =
            "java( assortment.getPurchaseOptions() == null ? null : " +
                    "assortment.getPurchaseOptions().stream()" +
                    ".map(po -> po.getId())" +
                    ".collect(java.util.stream.Collectors.toSet()) )")
    @Mapping(target = "locationIds", expression =
            "java( assortment.getAssortmentLocations() == null ? null : " +
                    "assortment.getAssortmentLocations().stream()" +
                    ".map(al -> al.getLocation().getId())" +
                    ".collect(java.util.stream.Collectors.toSet()) )")
    AssortmentDTO toDto(Assortment assortment);

    // If you also want to map DTO -> entity, you can create a method.
    // Typically you'd do that in the Service or selectively here,
    // but here's an example of partial:
    //
    // @Mapping(target = "inventoryItems", ignore = true)
    // @Mapping(target = "subRecipes", ignore = true)
    // @Mapping(target = "purchaseOptions", ignore = true)
    // @Mapping(target = "assortmentLocations", ignore = true)
    // @Mapping(target = "company", ignore = true) // set in service
    // Assortment toEntity(AssortmentDTO dto);

}
