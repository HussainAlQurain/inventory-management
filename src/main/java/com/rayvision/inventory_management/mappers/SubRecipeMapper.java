package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.SubRecipe;
import com.rayvision.inventory_management.model.dto.SubRecipeDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubRecipeMapper {
    // Entity → DTO
    @Mapping(source = "category.id",    target = "categoryId")
    @Mapping(source = "uom.id",         target = "uomId")
//    @Mapping(source = "company.id",     target = "companyId")  // only if you want/need that in DTO
    SubRecipeDTO toDto(SubRecipe entity);

    // DTO → Entity
    @Mapping(source = "categoryId", target = "category.id")
    @Mapping(source = "uomId",      target = "uom.id")
//    @Mapping(source = "companyId",  target = "company.id")
    SubRecipe toEntity(SubRecipeDTO dto);
}
