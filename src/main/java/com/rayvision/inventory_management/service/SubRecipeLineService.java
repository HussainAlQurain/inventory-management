package com.rayvision.inventory_management.service;

import com.rayvision.inventory_management.model.SubRecipeLine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SubRecipeLineService {
    List<SubRecipeLine> getLinesBySubRecipe(Long subRecipeId);
    
    /**
     * Get paginated lines for a subRecipe with optional search
     * @param subRecipeId The subRecipe ID
     * @param search Optional search term
     * @param pageable Pagination parameters
     * @return Paginated list of lines
     */
    Page<SubRecipeLine> getLinesBySubRecipe(Long subRecipeId, String search, Pageable pageable);

    Optional<SubRecipeLine> getOne(Long subRecipeId, Long lineId);

    SubRecipeLine createLine(Long subRecipeId, SubRecipeLine line);

    SubRecipeLine updateLine(Long subRecipeId, SubRecipeLine line);

    SubRecipeLine partialUpdateLine(Long subRecipeId, SubRecipeLine line);

    void deleteLine(Long subRecipeId, Long lineId);
}
