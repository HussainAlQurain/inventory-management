package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.MenuItemLine;
import com.rayvision.inventory_management.model.dto.MenuItemLineResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collection;
import java.util.List;

@Mapper(componentModel = "spring")
public interface MenuItemLineMapper {
    @Mapping(source = "id",               target = "id")
    @Mapping(source = "inventoryItem.id", target = "inventoryItemId")
    @Mapping(source = "subRecipe.id",     target = "subRecipeId")
    @Mapping(source = "childMenuItem.id", target = "childMenuItemId")  // match the field in your DTO
    @Mapping(source = "unitOfMeasure.id", target = "unitOfMeasureId")
    MenuItemLineResponseDTO toDto(MenuItemLine entity);

    List<MenuItemLineResponseDTO> toDtoList(Collection<MenuItemLine> entities);

    @Named("mapId")
    default Long mapId(Object entity) {
        if (entity == null) return null;
        try {
            return (Long) entity.getClass().getMethod("getId").invoke(entity);
        } catch (Exception e) {
            return null;
        }
    }

}
