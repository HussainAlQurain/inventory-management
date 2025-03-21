package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.SubRecipeItem;
import com.rayvision.inventory_management.model.dto.SubRecipeItemLineDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubRecipeItemLineMapper {
    // Entity → DTO
    @Mapping(source = "inventoryItem.id", target = "inventoryItemId")
    @Mapping(source = "unitOfMeasure.id", target = "unitOfMeasureId")
    SubRecipeItemLineDTO toDto(SubRecipeItem entity);

    // DTO → Entity
    @Mapping(source = "inventoryItemId", target = "inventoryItem.id")
    @Mapping(source = "unitOfMeasureId", target = "unitOfMeasure.id")
    SubRecipeItem toEntity(SubRecipeItemLineDTO dto);
}
