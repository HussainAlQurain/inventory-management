package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.MenuItem;
import com.rayvision.inventory_management.model.dto.MenuItemResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Converts MenuItem entities to MenuItemResponseDTO.
 * If you want to convert back from MenuItemResponseDTO → MenuItem,
 * you can add another method, but it's typically not needed for "response" DTO.
 */
@Mapper(componentModel = "spring", uses = {
        // If you have other mapper classes for category or line DTOs, list them here
        CategoryResponseMapper.class,
        MenuItemInventoryLineMapper.class,
        MenuItemSubRecipeLineMapper.class
})
public interface MenuItemResponseMapper {
    MenuItemResponseMapper INSTANCE = Mappers.getMapper(MenuItemResponseMapper.class);

    /**
     * Map MenuItem entity to MenuItemResponseDTO.
     * For nested objects:
     *  • category → categoryResponseDTO
     *  • menuItemInventoryItems → inventoryLines
     *  • menuItemSubRecipes → subRecipeLines
     */
    @Mapping(source = "category", target = "category")
    @Mapping(source = "menuItemInventoryItems", target = "inventoryLines")
    @Mapping(source = "menuItemSubRecipes", target = "subRecipeLines")
    MenuItemResponseDTO toDto(MenuItem entity);

}
