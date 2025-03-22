package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.MenuItemInventoryItem;
import com.rayvision.inventory_management.model.dto.MenuItemInventoryLineDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MenuItemInventoryLineMapper {
    @Mapping(source = "inventoryItem.id", target = "inventoryItemId")
    @Mapping(source = "unitOfMeasure.id", target = "unitOfMeasureId")
    @Mapping(source = "quantity",         target = "quantity")
    @Mapping(source = "wastagePercent",   target = "wastagePercent")
    MenuItemInventoryLineDTO toDto(MenuItemInventoryItem entity);
}
