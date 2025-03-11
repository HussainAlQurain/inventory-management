package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.InventoryItemLocation;
import com.rayvision.inventory_management.model.dto.InventoryItemLocationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryItemLocationMapper {
    @Mapping(source = "inventoryItem.id", target = "inventoryItemId")
    @Mapping(source = "location.id", target = "locationId")
    InventoryItemLocationDTO toDto(InventoryItemLocation entity);

    @Mapping(source = "inventoryItemId", target = "inventoryItem.id")
    @Mapping(source = "locationId", target = "location.id")
    InventoryItemLocation toEntity(InventoryItemLocationDTO dto);
}
