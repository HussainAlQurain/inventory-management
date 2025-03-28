package com.rayvision.inventory_management.mappers;

import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.SubRecipe;
import com.rayvision.inventory_management.model.SubRecipeLine;
import com.rayvision.inventory_management.model.UnitOfMeasure;
import com.rayvision.inventory_management.model.dto.SubRecipeLineDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface SubRecipeLineMapper {

    @Mapping(target = "childSubRecipe", source = "childSubRecipeId", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "inventoryItem", source = "inventoryItemId", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "unitOfMeasure", source = "unitOfMeasureId", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    SubRecipeLine toEntity(SubRecipeLineDTO dto);

    // Existing toDto mapping remains unchanged
    @Mapping(source = "inventoryItem.id", target = "inventoryItemId")
    @Mapping(source = "childSubRecipe.id", target = "childSubRecipeId")
    @Mapping(source = "unitOfMeasure.id", target = "unitOfMeasureId")
    SubRecipeLineDTO toDto(SubRecipeLine entity);

    // Add these custom mappings
    default SubRecipe mapChildSubRecipe(Long childSubRecipeId) {
        if (childSubRecipeId == null) {
            return null;
        }
        return SubRecipe.builder().id(childSubRecipeId).build();
    }

    default InventoryItem mapInventoryItem(Long inventoryItemId) {
        if (inventoryItemId == null) {
            return null;
        }
        return InventoryItem.builder().id(inventoryItemId).build();
    }

    default UnitOfMeasure mapUnitOfMeasure(Long unitOfMeasureId) {
        if (unitOfMeasureId == null) {
            return null;
        }
        return UnitOfMeasure.builder().id(unitOfMeasureId).build();
    }
}
