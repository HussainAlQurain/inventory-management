package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.CountUomPreference;
import com.rayvision.inventory_management.model.InventoryItem;
import com.rayvision.inventory_management.model.SubRecipe;
import com.rayvision.inventory_management.model.UnitOfMeasure;
import com.rayvision.inventory_management.repository.CountUomPreferenceRepository;
import com.rayvision.inventory_management.repository.InventoryItemRepository;
import com.rayvision.inventory_management.repository.SubRecipeRepository;
import com.rayvision.inventory_management.repository.UnitOfMeasureRepository;
import com.rayvision.inventory_management.service.CountUomPreferenceService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class CountUomPreferenceServiceImpl implements CountUomPreferenceService {
    private final CountUomPreferenceRepository preferenceRepository;
    private final UnitOfMeasureRepository uomRepository;
    private final InventoryItemRepository itemRepository;
    private final SubRecipeRepository subRecipeRepository;

    public CountUomPreferenceServiceImpl(
            CountUomPreferenceRepository preferenceRepository,
            UnitOfMeasureRepository uomRepository,
            InventoryItemRepository itemRepository,
            SubRecipeRepository subRecipeRepository
    ) {
        this.preferenceRepository = preferenceRepository;
        this.uomRepository = uomRepository;
        this.itemRepository = itemRepository;
        this.subRecipeRepository = subRecipeRepository;
    }

    // ------------------------------------------------------------
    // 1) For InventoryItem
    // ------------------------------------------------------------
    @Override
    public CountUomPreference addPreferenceForItem(Long itemId, Long uomId, boolean defaultUom) {
        InventoryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

        UnitOfMeasure uom = uomRepository.findById(uomId)
                .orElseThrow(() -> new RuntimeException("UOM not found: " + uomId));

        // optional: check that the UOM's category matches the item's inventoryUom category
        // e.g. item.getInventoryUom().getCategory().getId() == uom.getCategory().getId()
        if (!Objects.equals(
                item.getInventoryUom().getCategory().getId(),
                uom.getCategory().getId()
        )) {
            throw new RuntimeException("UOM category does not match the item's category");
        }

        // Check if already exists
        Optional<CountUomPreference> existing = preferenceRepository
                .findByInventoryItemIdAndCountUomId(itemId, uomId);
        if (existing.isPresent()) {
            // Possibly update defaultUom if you want
            CountUomPreference pref = existing.get();
            pref.setDefaultUom(defaultUom);
            return preferenceRepository.save(pref);
        }

        // If not exist => create new
        CountUomPreference pref = new CountUomPreference();
        pref.setInventoryItem(item);
        pref.setSubRecipe(null);
        pref.setCountUom(uom);
        pref.setDefaultUom(defaultUom);

        return preferenceRepository.save(pref);
    }

    @Override
    public void removePreferenceForItem(Long itemId, Long uomId) {
        // find and delete
        Optional<CountUomPreference> maybePref = preferenceRepository
                .findByInventoryItemIdAndCountUomId(itemId, uomId);
        if (maybePref.isPresent()) {
            preferenceRepository.delete(maybePref.get());
        }
    }

    @Override
    public List<CountUomPreference> getPreferencesForItem(Long itemId) {
        return preferenceRepository.findByInventoryItemId(itemId);
    }

    // ------------------------------------------------------------
    // 2) For SubRecipe
    // ------------------------------------------------------------
    @Override
    public CountUomPreference addPreferenceForSubRecipe(Long subRecipeId, Long uomId, boolean defaultUom) {
        SubRecipe sub = subRecipeRepository.findById(subRecipeId)
                .orElseThrow(() -> new RuntimeException("SubRecipe not found: " + subRecipeId));

        UnitOfMeasure uom = uomRepository.findById(uomId)
                .orElseThrow(() -> new RuntimeException("UOM not found: " + uomId));

        // optional: check that the UOM's category matches the subRecipe's base UOM category
        if (!Objects.equals(
                sub.getUom().getCategory().getId(),
                uom.getCategory().getId()
        )) {
            throw new RuntimeException("UOM category does not match the subRecipe's category");
        }

        // check if existing
        Optional<CountUomPreference> existing = preferenceRepository
                .findBySubRecipeIdAndCountUomId(subRecipeId, uomId);
        if (existing.isPresent()) {
            CountUomPreference pref = existing.get();
            pref.setDefaultUom(defaultUom);
            return preferenceRepository.save(pref);
        }

        // create new
        CountUomPreference pref = new CountUomPreference();
        pref.setSubRecipe(sub);
        pref.setInventoryItem(null);
        pref.setCountUom(uom);
        pref.setDefaultUom(defaultUom);

        return preferenceRepository.save(pref);
    }

    @Override
    public void removePreferenceForSubRecipe(Long subRecipeId, Long uomId) {
        Optional<CountUomPreference> maybePref = preferenceRepository
                .findBySubRecipeIdAndCountUomId(subRecipeId, uomId);
        maybePref.ifPresent(preferenceRepository::delete);
    }

    @Override
    public List<CountUomPreference> getPreferencesForSubRecipe(Long subRecipeId) {
        return preferenceRepository.findBySubRecipeId(subRecipeId);
    }

    // ------------------------------------------------------------
    // 3) Getting "available UOMs" with same category
    // ------------------------------------------------------------
    @Override
    public List<UnitOfMeasure> getAvailableUomsForItem(Long itemId) {
        InventoryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

        // We find all UOM in that same category, for the same company
        Long catId = item.getInventoryUom().getCategory().getId();
        Long companyId = item.getCompany().getId();

        return uomRepository.findByCategoryAndCompany(catId, companyId);
    }

    @Override
    public List<UnitOfMeasure> getAvailableUomsForSubRecipe(Long subRecipeId) {
        SubRecipe sub = subRecipeRepository.findById(subRecipeId)
                .orElseThrow(() -> new RuntimeException("SubRecipe not found: " + subRecipeId));

        Long catId = sub.getUom().getCategory().getId();
        Long companyId = sub.getCompany().getId();

        return uomRepository.findByCategoryAndCompany(catId, companyId);
    }

}
