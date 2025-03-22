package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.MenuItemSubRecipe;
import com.rayvision.inventory_management.model.dto.MenuItemSubRecipeLineDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MenuItemSubRecipeLineMapper {
    @Mapping(source = "subRecipe.id",      target = "subRecipeId")
    @Mapping(source = "unitOfMeasure.id",  target = "unitOfMeasureId")
    @Mapping(source = "quantity",          target = "quantity")
    @Mapping(source = "wastagePercent",    target = "wastagePercent")
    MenuItemSubRecipeLineDTO toDto(MenuItemSubRecipe entity);
}
