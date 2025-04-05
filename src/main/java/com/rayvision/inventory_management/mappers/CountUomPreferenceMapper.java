package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.CountUomPreference;
import com.rayvision.inventory_management.model.dto.CountUomPreferenceDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CountUomPreferenceMapper {
    // Entity -> DTO
    @Mapping(source = "inventoryItem.id", target = "inventoryItemId")
    @Mapping(source = "subRecipe.id", target = "subRecipeId")
    @Mapping(source = "countUom.id", target = "countUomId")
    @Mapping(source = "defaultUom", target = "defaultUom")
    CountUomPreferenceDTO toDto(CountUomPreference entity);

    // DTO -> Entity
    // Usually we don't blindly map IDs to entity relationships, because
    // we rely on the service to fetch the actual item/subRecipe/UOM.
    // So we can skip them here or set them to null, letting the service fill them in.
    @Mapping(target = "inventoryItem", ignore = true)
    @Mapping(target = "subRecipe", ignore = true)
    @Mapping(target = "countUom", ignore = true)
    @Mapping(target = "defaultUom", source = "defaultUom")
    CountUomPreference toEntity(CountUomPreferenceDTO dto);

}
