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
    private final PurchaseOptionRepository purchaseOptionRepository;

    public AssortmentServiceImpl(
            AssortmentRepository assortmentRepository,
            CompanyRepository companyRepository,
            InventoryItemRepository inventoryItemRepository,
            SubRecipeRepository subRecipeRepository,
            LocationRepository locationRepository,
            AssortmentLocationRepository assortmentLocationRepository,
            PurchaseOptionRepository purchaseOptionRepository
    ) {
        this.assortmentRepository = assortmentRepository;
        this.companyRepository = companyRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.subRecipeRepository = subRecipeRepository;
        this.locationRepository = locationRepository;
        this.assortmentLocationRepository = assortmentLocationRepository;
        this.purchaseOptionRepository = purchaseOptionRepository;
    }

    // ----------------------------------------------------
    // 1) CREATE with name
    // ----------------------------------------------------
    @Override
    public Assortment create(Long companyId, String name) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found: " + companyId));

        Assortment assortment = new Assortment();
        assortment.setName(name);
        assortment.setCompany(company);
        assortment.setInventoryItems(new HashSet<>());
        assortment.setSubRecipes(new HashSet<>());
        assortment.setPurchaseOptions(new HashSet<>());
        assortment.setAssortmentLocations(new HashSet<>());

        return assortmentRepository.save(assortment);
    }

    // ----------------------------------------------------
    // 2) GET one or all
    // ----------------------------------------------------
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

    // ----------------------------------------------------
    // 3) DELETE
    // ----------------------------------------------------
    @Override
    public void delete(Long companyId, Long assortmentId) {
        Assortment existing = getOne(companyId, assortmentId);
        assortmentRepository.delete(existing);
    }

    // ----------------------------------------------------
    // 4) Full or Partial Update
    // (Keep your existing logic if you want.)
    // ----------------------------------------------------
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
        // Overwrite subRecipes
        if (dto.getPurchaseOptionIds() != null) {
            Set<PurchaseOption> purchaseOptions = fetchPurchaseOptions(dto.getPurchaseOptionIds());
            existing.setPurchaseOptions(purchaseOptions);
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
        if (dto.getPurchaseOptionIds() != null) {
            Set<PurchaseOption> purchaseOptions = fetchPurchaseOptions(dto.getPurchaseOptionIds());
            existing.setPurchaseOptions(purchaseOptions);
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

    // ----------------------------------------------------
    // 5) Bulk Add/Remove: InventoryItems
    // ----------------------------------------------------
    @Override
    public Assortment addInventoryItems(Long companyId, Long assortmentId, Set<Long> itemIds) {
        Assortment assortment = getOne(companyId, assortmentId);
        if (itemIds == null || itemIds.isEmpty()) {
            return assortment; // nothing to add
        }

        Set<InventoryItem> itemsToAdd = fetchInventoryItems(itemIds);
        assortment.getInventoryItems().addAll(itemsToAdd);
        // Just calling addAll(...) on the set is enough; no bridging table for items
        return assortmentRepository.save(assortment);
    }

    @Override
    public Assortment removeInventoryItems(Long companyId, Long assortmentId, Set<Long> itemIds) {
        Assortment assortment = getOne(companyId, assortmentId);
        if (itemIds == null || itemIds.isEmpty()) {
            return assortment; // nothing to remove
        }

        Set<InventoryItem> itemsToRemove = fetchInventoryItems(itemIds);
        assortment.getInventoryItems().removeAll(itemsToRemove);
        return assortmentRepository.save(assortment);
    }

    // ----------------------------------------------------
    // 6) Bulk Add/Remove: SubRecipes
    // ----------------------------------------------------
    @Override
    public Assortment addSubRecipes(Long companyId, Long assortmentId, Set<Long> subRecipeIds) {
        Assortment assortment = getOne(companyId, assortmentId);
        if (subRecipeIds == null || subRecipeIds.isEmpty()) {
            return assortment;
        }

        Set<SubRecipe> subsToAdd = fetchSubRecipes(subRecipeIds);
        assortment.getSubRecipes().addAll(subsToAdd);
        return assortmentRepository.save(assortment);
    }

    @Override
    public Assortment removeSubRecipes(Long companyId, Long assortmentId, Set<Long> subRecipeIds) {
        Assortment assortment = getOne(companyId, assortmentId);
        if (subRecipeIds == null || subRecipeIds.isEmpty()) {
            return assortment;
        }

        Set<SubRecipe> subsToRemove = fetchSubRecipes(subRecipeIds);
        assortment.getSubRecipes().removeAll(subsToRemove);
        return assortmentRepository.save(assortment);
    }

    // ----------------------------------------------------
    // 7) Bulk Add/Remove: PurchaseOptions
    // ----------------------------------------------------
    @Override
    public Assortment addPurchaseOptions(Long companyId, Long assortmentId, Set<Long> purchaseOptionIds) {
        Assortment assortment = getOne(companyId, assortmentId);
        if (purchaseOptionIds == null || purchaseOptionIds.isEmpty()) {
            return assortment;
        }

        Set<PurchaseOption> poToAdd = fetchPurchaseOptions(purchaseOptionIds);
        assortment.getPurchaseOptions().addAll(poToAdd);
        return assortmentRepository.save(assortment);
    }

    @Override
    public Assortment removePurchaseOptions(Long companyId, Long assortmentId, Set<Long> purchaseOptionIds) {
        Assortment assortment = getOne(companyId, assortmentId);
        if (purchaseOptionIds == null || purchaseOptionIds.isEmpty()) {
            return assortment;
        }

        Set<PurchaseOption> poToRemove = fetchPurchaseOptions(purchaseOptionIds);
        assortment.getPurchaseOptions().removeAll(poToRemove);
        return assortmentRepository.save(assortment);
    }

    // ----------------------------------------------------
    // 8) Bulk Add/Remove: Locations (via bridging)
    // ----------------------------------------------------
    @Override
    public Assortment addLocations(Long companyId, Long assortmentId, Set<Long> locationIds) {
        Assortment assortment = getOne(companyId, assortmentId);
        if (locationIds == null || locationIds.isEmpty()) {
            return assortment;
        }

        for (Long locId : locationIds) {
            Location location = locationRepository.findById(locId)
                    .orElseThrow(() -> new RuntimeException("Location not found: " + locId));

            // Check if bridging already exists
            boolean alreadyLinked = assortment.getAssortmentLocations().stream()
                    .anyMatch(al -> al.getLocation().getId().equals(locId));
            if (!alreadyLinked) {
                // create new bridging
                AssortmentLocation al = new AssortmentLocation();
                al.setAssortment(assortment);
                al.setLocation(location);
                assortmentLocationRepository.save(al);

                // also add to the in-memory set
                assortment.getAssortmentLocations().add(al);
            }
        }
        // Return the updated entity
        return assortmentRepository.save(assortment);
    }

    @Override
    public Assortment removeLocations(Long companyId, Long assortmentId, Set<Long> locationIds) {
        Assortment assortment = getOne(companyId, assortmentId);
        if (locationIds == null || locationIds.isEmpty()) {
            return assortment;
        }

        // find bridging records that match the given location IDs
        Set<AssortmentLocation> toRemove = new HashSet<>();
        for (AssortmentLocation al : assortment.getAssortmentLocations()) {
            if (locationIds.contains(al.getLocation().getId())) {
                toRemove.add(al);
            }
        }
        // remove from DB
        assortmentLocationRepository.deleteAll(toRemove);
        // remove from memory
        assortment.getAssortmentLocations().removeAll(toRemove);

        return assortmentRepository.save(assortment);
    }

    // ------------------------------------------------------------------------
    // HELPER METHODS
    // ------------------------------------------------------------------------

    private Set<InventoryItem> fetchInventoryItems(Set<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) return new HashSet<>();
        List<InventoryItem> found = inventoryItemRepository.findAllById(itemIds);
        checkMissingIds("InventoryItem", itemIds, found.stream().map(InventoryItem::getId).toList());
        return new HashSet<>(found);
    }

    private Set<SubRecipe> fetchSubRecipes(Set<Long> subRecipeIds) {
        if (subRecipeIds == null || subRecipeIds.isEmpty()) return new HashSet<>();
        List<SubRecipe> found = subRecipeRepository.findAllById(subRecipeIds);
        checkMissingIds("SubRecipe", subRecipeIds, found.stream().map(SubRecipe::getId).toList());
        return new HashSet<>(found);
    }

    private Set<PurchaseOption> fetchPurchaseOptions(Set<Long> poIds) {
        if (poIds == null || poIds.isEmpty()) return new HashSet<>();
        List<PurchaseOption> found = purchaseOptionRepository.findAllById(poIds);
        checkMissingIds("PurchaseOption", poIds, found.stream().map(PurchaseOption::getId).toList());
        return new HashSet<>(found);
    }

    private <T> void checkMissingIds(String entityName, Set<Long> requestedIds, List<Long> foundIds) {
        Set<Long> missing = new HashSet<>(requestedIds);
        missing.removeAll(foundIds);
        if (!missing.isEmpty()) {
            throw new RuntimeException("Some " + entityName + " IDs not found: " + missing);
        }
    }

}
