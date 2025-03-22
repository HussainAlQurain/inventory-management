package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.Category;
import com.rayvision.inventory_management.model.dto.CategoryResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryResponseMapper {
    // Simple entity → DTO for Category
    @Mapping(source = "id",          target = "id")
    @Mapping(source = "name",        target = "name")
    @Mapping(source = "description", target = "description")
    CategoryResponseDTO toDto(Category entity);
}
