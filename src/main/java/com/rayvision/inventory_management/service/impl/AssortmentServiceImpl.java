package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.AssortmentDTO;
import com.rayvision.inventory_management.repository.*;
import com.rayvision.inventory_management.service.AssortmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AssortmentServiceImpl implements AssortmentService {
    private final AssortmentRepository assortmentRepository;
    private final CompanyRepository companyRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SubRecipeRepository subRecipeRepository;
    private final LocationRepository locationRepository;
    private final AssortmentLocationRepository assortmentLocationRepository;

    public AssortmentServiceImpl(AssortmentRepository assortmentRepository,
                                 CompanyRepository companyRepository,
                                 InventoryItemRepository inventoryItemRepository,
                                 SubRecipeRepository subRecipeRepository,
                                 LocationRepository locationRepository,
                                 AssortmentLocationRepository assortmentLocationRepository) {
        this.assortmentRepository = assortmentRepository;
        this.companyRepository = companyRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.subRecipeRepository = subRecipeRepository;
        this.locationRepository = locationRepository;
        this.assortmentLocationRepository = assortmentLocationRepository;
    }

    @Override
    public Assortment create(Long companyId, AssortmentDTO dto) {
        // 1) Validate company
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found: " + companyId));

        // 2) Build new Assortment
        Assortment assortment = new Assortment();
        assortment.setName(dto.getName());
        assortment.setCompany(company);

        // 3) Link inventoryItems
        if (dto.getItemIds() != null && !dto.getItemIds().isEmpty()) {
            Set<InventoryItem> items = fetchInventoryItems(dto.getItemIds());
            assortment.setInventoryItems(items);
        } else {
            assortment.setInventoryItems(new HashSet<>());
        }

        // 4) Link subRecipes
        if (dto.getSubRecipeIds() != null && !dto.getSubRecipeIds().isEmpty()) {
            Set<SubRecipe> subRecipes = fetchSubRecipes(dto.getSubRecipeIds());
            assortment.setSubRecipes(subRecipes);
        } else {
            assortment.setSubRecipes(new HashSet<>());
        }

        // 5) We'll handle location bridging after we save
        assortment.setAssortmentLocations(new HashSet<>());
        Assortment saved = assortmentRepository.save(assortment);

        // 6) Now create bridging for locationIds
        if (dto.getLocationIds() != null && !dto.getLocationIds().isEmpty()) {
            for (Long locId : dto.getLocationIds()) {
                Location location = locationRepository.findById(locId)
                        .orElseThrow(() -> new RuntimeException("Location not found: " + locId));

                // Create bridging
                AssortmentLocation al = new AssortmentLocation();
                al.setAssortment(saved);
                al.setLocation(location);
                assortmentLocationRepository.save(al);
                // saved.getAssortmentLocations().add(al);
            }
        }
        return saved;
    }

    @Override
    public Assortment update(Long companyId, Long assortmentId, AssortmentDTO dto) {
        // Full update
        Assortment existing = getOne(companyId, assortmentId);

        existing.setName(dto.getName()); // overwriting name
        // Overwrite items
        if (dto.getItemIds() != null) {
            Set<InventoryItem> items = fetchInventoryItems(dto.getItemIds());
            existing.setInventoryItems(items);
        }
        // Overwrite subRecipes
        if (dto.getSubRecipeIds() != null) {
            Set<SubRecipe> subs = fetchSubRecipes(dto.getSubRecipeIds());
            existing.setSubRecipes(subs);
        }

        // Overwrite location bridging: remove old, add new
        if (dto.getLocationIds() != null) {
            // 1) remove existing bridging
            assortmentLocationRepository.deleteAll(existing.getAssortmentLocations());
            existing.getAssortmentLocations().clear();

            // 2) create new bridging
            for (Long locId : dto.getLocationIds()) {
                Location loc = locationRepository.findById(locId)
                        .orElseThrow(() -> new RuntimeException("Location not found: " + locId));
                AssortmentLocation al = new AssortmentLocation();
                al.setAssortment(existing);
                al.setLocation(loc);
                assortmentLocationRepository.save(al);
            }
        }
        return assortmentRepository.save(existing);
    }

    @Override
    public Assortment partialUpdate(Long companyId, Long assortmentId, AssortmentDTO dto) {
        // partial, only update if not null
        Assortment existing = getOne(companyId, assortmentId);

        if (dto.getName() != null) {
            existing.setName(dto.getName());
        }
        // If itemIds is not null, set them
        if (dto.getItemIds() != null) {
            Set<InventoryItem> items = fetchInventoryItems(dto.getItemIds());
            existing.setInventoryItems(items);
        }
        // subRecipes
        if (dto.getSubRecipeIds() != null) {
            Set<SubRecipe> subs = fetchSubRecipes(dto.getSubRecipeIds());
            existing.setSubRecipes(subs);
        }
        // location bridging
        if (dto.getLocationIds() != null) {
            // remove old bridging
            assortmentLocationRepository.deleteAll(existing.getAssortmentLocations());
            existing.getAssortmentLocations().clear();

            // create new bridging
            for (Long locId : dto.getLocationIds()) {
                Location loc = locationRepository.findById(locId)
                        .orElseThrow(() -> new RuntimeException("Location not found: " + locId));
                AssortmentLocation al = new AssortmentLocation();
                al.setAssortment(existing);
                al.setLocation(loc);
                assortmentLocationRepository.save(al);
            }
        }
        return assortmentRepository.save(existing);
    }

    @Override
    public void delete(Long companyId, Long assortmentId) {
        Assortment existing = getOne(companyId, assortmentId);
        assortmentRepository.delete(existing);
    }

    @Override
    public Assortment getOne(Long companyId, Long assortmentId) {
        return assortmentRepository.findById(assortmentId)
                .filter(a -> a.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new RuntimeException("Assortment not found or not in this company"));
    }

    @Override
    public List<Assortment> getAll(Long companyId) {
        return assortmentRepository.findByCompanyId(companyId);
    }

    // ------------------------------------------------------------------------
    // Helper methods to fetch items, subRecipes
    // ------------------------------------------------------------------------
    private Set<InventoryItem> fetchInventoryItems(Set<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return new HashSet<>();
        }
        List<InventoryItem> found = inventoryItemRepository.findAllById(itemIds);
        // check for missing IDs
        Set<Long> foundIds = new HashSet<>();
        for (InventoryItem i : found) {
            foundIds.add(i.getId());
        }
        Set<Long> missing = new HashSet<>(itemIds);
        missing.removeAll(foundIds);
        if (!missing.isEmpty()) {
            throw new RuntimeException("Some InventoryItem IDs not found: " + missing);
        }
        return new HashSet<>(found);
    }

    private Set<SubRecipe> fetchSubRecipes(Set<Long> subRecipeIds) {
        if (subRecipeIds.isEmpty()) {
            return new HashSet<>();
        }
        List<SubRecipe> found = subRecipeRepository.findAllById(subRecipeIds);
        Set<Long> foundIds = new HashSet<>();
        for (SubRecipe s : found) {
            foundIds.add(s.getId());
        }
        Set<Long> missing = new HashSet<>(subRecipeIds);
        missing.removeAll(foundIds);
        if (!missing.isEmpty()) {
            throw new RuntimeException("Some SubRecipe IDs not found: " + missing);
        }
        return new HashSet<>(found);
    }

}
