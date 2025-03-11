package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.PrepItemLocation;
import com.rayvision.inventory_management.model.dto.PrepItemLocationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PrepItemLocationMapper {
    // Entity -> DTO
    @Mapping(source = "subRecipe.id", target = "subRecipeId")
    @Mapping(source = "location.id", target = "locationId")
    PrepItemLocationDTO toDto(PrepItemLocation entity);

    // DTO -> Entity
    @Mapping(source = "subRecipeId", target = "subRecipe.id")
    @Mapping(source = "locationId", target = "location.id")
    PrepItemLocation toEntity(PrepItemLocationDTO dto);

}
