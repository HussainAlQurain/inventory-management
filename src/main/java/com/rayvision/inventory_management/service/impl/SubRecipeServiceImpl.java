package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.Category;
import com.rayvision.inventory_management.model.Company;
import com.rayvision.inventory_management.model.SubRecipe;
import com.rayvision.inventory_management.repository.CategoryRepository;
import com.rayvision.inventory_management.repository.CompanyRepository;
import com.rayvision.inventory_management.repository.SubRecipeRepository;
import com.rayvision.inventory_management.service.SubRecipeService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubRecipeServiceImpl implements SubRecipeService {
    private final SubRecipeRepository subRecipeRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;

    public SubRecipeServiceImpl(SubRecipeRepository subRecipeRepository,
                                CompanyRepository companyRepository,
                                CategoryRepository categoryRepository) {
        this.subRecipeRepository = subRecipeRepository;
        this.companyRepository = companyRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<SubRecipe> getAllSubRecipes(Long companyId) {
        return subRecipeRepository.findByCompanyId(companyId);
    }

    @Override
    public Optional<SubRecipe> getSubRecipeById(Long companyId, Long subRecipeId) {
        return subRecipeRepository.findById(subRecipeId)
                .filter(sr -> sr.getCompany().getId().equals(companyId)); //filter might not make a difference if we're using single app
    }

    @Override
    public SubRecipe createSubRecipe(Long companyId, SubRecipe subRecipe) {
        // (1) Ensure the company is valid
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Invalid company ID: " + companyId));
        subRecipe.setCompany(company);

        // (2) Optionally ensure subRecipe.category belongs to same company
        if (subRecipe.getCategory() != null) {
            // fetch from DB:
            Long catId = subRecipe.getCategory().getId();
            Category cat = categoryRepository.findByCompanyIdAndId(companyId, catId)
                    .orElseThrow(() -> new RuntimeException("Category not found for id: " + catId));
            subRecipe.setCategory(cat);
        }

        // (3) Save
        return subRecipeRepository.save(subRecipe);
    }

    @Override
    public SubRecipe updateSubRecipe(Long companyId, SubRecipe subRecipe) {
        // must exist
        SubRecipe existing = subRecipeRepository.findById(subRecipe.getId())
                .filter(sr -> sr.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new RuntimeException(
                        "SubRecipe not found for id: " + subRecipe.getId() + " or does not belong to company " + companyId
                ));

        // update fields fully
        existing.setName(subRecipe.getName());
        existing.setType(subRecipe.getType());
        existing.setYieldQty(subRecipe.getYieldQty());
        existing.setPhotoUrl(subRecipe.getPhotoUrl());
        existing.setPrepTimeMinutes(subRecipe.getPrepTimeMinutes());
        existing.setCookTimeMinutes(subRecipe.getCookTimeMinutes());
        existing.setInstructions(subRecipe.getInstructions());
        existing.setCost(subRecipe.getCost());

        // If user changed category, do similar logic
        if (subRecipe.getCategory() != null) {
            Long newCatId = subRecipe.getCategory().getId();
            Category cat = categoryRepository.findByCompanyIdAndId(companyId, newCatId)
                    .orElseThrow(() -> new RuntimeException("Category not found for: " + newCatId));
            existing.setCategory(cat);
        }

        // update UOM similarly
        if (subRecipe.getUom() != null) {
            // do your checks that UOM belongs to same company, etc.
            existing.setUom(subRecipe.getUom());
        }

        return subRecipeRepository.save(existing);
    }

    @Override
    public SubRecipe partialUpdateSubRecipe(Long companyId, SubRecipe subRecipe) {
        // must exist
        SubRecipe existing = subRecipeRepository.findById(subRecipe.getId())
                .filter(sr -> sr.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new RuntimeException(
                        "SubRecipe not found or not in this company: " + subRecipe.getId()
                ));

        // only update if not null
        if (subRecipe.getName() != null) existing.setName(subRecipe.getName());
        if (subRecipe.getType() != null) existing.setType(subRecipe.getType());
        if (subRecipe.getYieldQty() != null) existing.setYieldQty(subRecipe.getYieldQty());
        if (subRecipe.getPhotoUrl() != null) existing.setPhotoUrl(subRecipe.getPhotoUrl());
        if (subRecipe.getPrepTimeMinutes() != null) existing.setPrepTimeMinutes(subRecipe.getPrepTimeMinutes());
        if (subRecipe.getCookTimeMinutes() != null) existing.setCookTimeMinutes(subRecipe.getCookTimeMinutes());
        if (subRecipe.getInstructions() != null) existing.setInstructions(subRecipe.getInstructions());
        if (subRecipe.getCost() != null) existing.setCost(subRecipe.getCost());

        if (subRecipe.getCategory() != null) {
            Long newCatId = subRecipe.getCategory().getId();
            Category cat = categoryRepository.findByCompanyIdAndId(companyId, newCatId)
                    .orElseThrow(() -> new RuntimeException("Category not found for: " + newCatId));
            existing.setCategory(cat);
        }

        if (subRecipe.getUom() != null) {
            existing.setUom(subRecipe.getUom());
        }

        return subRecipeRepository.save(existing);
    }

    @Override
    public void deleteSubRecipeById(Long companyId, Long subRecipeId) {
        SubRecipe existing = subRecipeRepository.findById(subRecipeId)
                .filter(sr -> sr.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new RuntimeException(
                        "Cannot find subRecipe for id: " + subRecipeId + " or not in company " + companyId
                ));
        subRecipeRepository.delete(existing);
    }

}
