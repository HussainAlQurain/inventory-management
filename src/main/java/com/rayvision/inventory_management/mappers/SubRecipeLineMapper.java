package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.SubRecipeLine;
import com.rayvision.inventory_management.model.dto.SubRecipeLineDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubRecipeLineMapper {
    @Mapping(source = "inventoryItem.id",     target = "inventoryItemId")
    @Mapping(source = "childSubRecipe.id",    target = "childSubRecipeId")
    @Mapping(source = "unitOfMeasure.id",     target = "unitOfMeasureId")
    SubRecipeLineDTO toDto(SubRecipeLine entity);

    @Mapping(source = "inventoryItemId",    target = "inventoryItem.id")
    @Mapping(source = "childSubRecipeId",   target = "childSubRecipe.id")
    @Mapping(source = "unitOfMeasureId",    target = "unitOfMeasure.id")
    SubRecipeLine toEntity(SubRecipeLineDTO dto);
}
