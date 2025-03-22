package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.SubRecipeLine;

import java.util.List;
import java.util.Optional;

public interface SubRecipeLineService {
    List<SubRecipeLine> getLinesBySubRecipe(Long subRecipeId);

    Optional<SubRecipeLine> getOne(Long subRecipeId, Long lineId);

    SubRecipeLine createLine(Long subRecipeId, SubRecipeLine line);

    SubRecipeLine updateLine(Long subRecipeId, SubRecipeLine line);

    SubRecipeLine partialUpdateLine(Long subRecipeId, SubRecipeLine line);

    void deleteLine(Long subRecipeId, Long lineId);
}
