package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.OrderCreateDTO;
import com.rayvision.inventory_management.model.dto.OrderItemDTO;
import com.rayvision.inventory_management.repository.AssortmentLocationRepository;
import com.rayvision.inventory_management.repository.AutoOrderSettingRepository;
import com.rayvision.inventory_management.repository.InventoryItemRepository;
import com.rayvision.inventory_management.repository.LocationRepository;
import com.rayvision.inventory_management.repository.UserRepository;
import com.rayvision.inventory_management.service.InventoryItemLocationService;
import com.rayvision.inventory_management.service.PurchaseOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class AutoOrderScheduledService {
    private final AutoOrderSettingRepository settingRepo;
    private final InventoryItemLocationService itemLocationService;
    private final PurchaseOrderService purchaseOrderService;
    private final NotificationService notificationService;
    private final LocationRepository locationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final AssortmentLocationRepository assortmentLocationRepository;
    private final UserRepository userRepository;

    public AutoOrderScheduledService(
            AutoOrderSettingRepository settingRepo,
            InventoryItemLocationService itemLocationService,
            PurchaseOrderService purchaseOrderService,
            NotificationService notificationService,
            LocationRepository locationRepository,
            InventoryItemRepository inventoryItemRepository,
            AssortmentLocationRepository assortmentLocationRepository,
            UserRepository userRepository
    ) {
        this.settingRepo = settingRepo;
        this.itemLocationService = itemLocationService;
        this.purchaseOrderService = purchaseOrderService;
        this.notificationService = notificationService;
        this.locationRepository = locationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.assortmentLocationRepository = assortmentLocationRepository;
        this.userRepository = userRepository;
    }

    /* ------------------------------------------------------------------
       Core job execution method - called by DynamicSchedulerService
     ------------------------------------------------------------------ */
    @Async("autoOrderExecutor")
    public CompletableFuture<Boolean> processAutoOrderAsync(Long settingId) {
        try {
            // Use the eager loading repository method
            AutoOrderSetting setting = settingRepo.findByIdWithLocationAndCompany(settingId)
                .orElseThrow(() -> new RuntimeException("Setting not found: " + settingId));
            
            // Skip if setting is disabled
            if (!setting.isEnabled()) {
                log.debug("Skipping auto-order for setting ID {} - disabled", settingId);
                return CompletableFuture.completedFuture(false);
            }
            
            // Run the core logic
            runAutoOrderLogic(setting);
            
            // Update last check time
            updateLastCheckTime(settingId);
            
            return CompletableFuture.completedFuture(true);
        } catch (Exception ex) {
            log.error("Auto-order process failed for setting ID: {}", settingId, ex);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    @Transactional
    public void updateLastCheckTime(Long settingId) {
        try {
            AutoOrderSetting freshSetting = settingRepo.findById(settingId)
                .orElseThrow(() -> new RuntimeException("Setting not found: " + settingId));
            freshSetting.setLastCheckTime(LocalDateTime.now());
            settingRepo.save(freshSetting);
            log.debug("Updated lastCheckTime for setting ID: {}", settingId);
        } catch (Exception ex) {
            log.error("Failed to update lastCheckTime for setting ID: {}", settingId, ex);
        }
    }

    @Transactional
    public void runAutoOrderLogic(AutoOrderSetting setting) {
        // Re-fetch location to avoid LazyInitialization
        Location loc = null;
        if (setting.getLocation() != null) {
            Long locationId = setting.getLocation().getId();
            loc = locationRepository.findById(locationId)
                .orElse(null);
            
            if (loc == null) {
                log.warn("Location with ID {} not found", locationId);
                return;
            }
        } else {
            log.warn("Location is null for setting ID: {}", setting.getId());
            return;
        }

        log.debug("Running auto-order logic for location: {} - {}", loc.getId(), loc.getName());
        LocalDateTime startTime = LocalDateTime.now();

        // 1) figure out which items are in scope
        // Re-fetch company to avoid LazyInitialization
        Company company = loc.getCompany();
        if (company == null) {
            log.warn("Company is null for location ID: {}", loc.getId());
            return;
        }
        
        Long companyId = company.getId();
        List<AssortmentLocation> bridgingAssortments =
                assortmentLocationRepository.findByLocationId(loc.getId());

        Set<InventoryItem> unionItems = new HashSet<>();
        if (bridgingAssortments.isEmpty()) {
            // fallback to ALL items in the company
            unionItems.addAll(inventoryItemRepository.findByCompanyId(companyId));
            log.debug("No assortments found, using all company items: {}", unionItems.size());
        } else {
            for (AssortmentLocation al : bridgingAssortments) {
                // Eager fetch the inventory items to avoid lazy loading issues
                Assortment assortment = al.getAssortment();
                if (assortment != null) {
                    unionItems.addAll(assortment.getInventoryItems());
                }
            }
            log.debug("Using items from {} assortments, total items: {}",
                    bridgingAssortments.size(), unionItems.size());
        }

        // 2) get itemLocation bridging rows
        List<InventoryItemLocation> bridgingList = itemLocationService.getByLocation(loc.getId());
        if (bridgingList.isEmpty()) {
            log.warn("No inventory item location bridging rows found for location: {}", loc.getId());
            return; // no items
        }

        // build a map itemId -> bridging row
        Map<Long, InventoryItemLocation> iilMap = new HashMap<>();
        for (InventoryItemLocation bil : bridgingList) {
            iilMap.put(bil.getInventoryItem().getId(), bil);
        }

        // 2.5) Get in-transit quantities from sent orders
        Map<Long, Double> inTransitMap = purchaseOrderService.calculateInTransitQuantitiesByLocation(loc.getId());
        log.debug("Found {} items with in-transit quantities", inTransitMap.size());

        // 3) For each item in unionItems => compute shortage if par>0
        Map<Supplier, List<ShortageLine>> shortageMap = new HashMap<>();
        int itemsProcessed = 0;
        int shortagesFound = 0;

        for (InventoryItem item : unionItems) {
            itemsProcessed++;
            InventoryItemLocation bil = iilMap.get(item.getId());
            if (bil == null) {
                // no bridging row => skip
                continue;
            }

            // if user wants "skip items with both minOnHand & par=0"
            double min = (bil.getMinOnHand() != null) ? bil.getMinOnHand() : 0.0;
            double par = (bil.getParLevel() != null) ? bil.getParLevel() : 0.0;
            if ((min <= 0) && (par <= 0)) {
                // skip if both are effectively zero => user doesn't want auto-order
                continue;
            }

            // compute shortage from par - onHand
            double onHand = (bil.getOnHand() != null) ? bil.getOnHand() : 0.0;

            // Account for in-transit quantities (already ordered but not received)
            double inTransitQty = inTransitMap.getOrDefault(item.getId(), 0.0);

            // Total expected quantity = current on hand + in transit
            double effectiveOnHand = onHand + inTransitQty;

            // Calculate shortage based on effective on-hand (including in-transit)
            double shortage = par - effectiveOnHand;

            if (shortage <= 0) {
                // no ordering needed
                continue;
            }

            shortagesFound++;
            log.debug("Item {} - {} has shortage of {} (Par: {}, OnHand: {}, In-transit: {}, Min: {})",
                    item.getId(), item.getName(), shortage, par, onHand, inTransitQty, min);

            // 4) pick an enabled purchase option => get supplier
            PurchaseOption po = pickEnabledMainOrFallbackPurchaseOption(item);
            if (po == null) {
                // no valid option => create a notification to user
                log.info("No enabled purchase option found for item: {} - {}", item.getId(), item.getName());
                notificationService.createNotification(
                        companyId,
                        "Auto-Order Skipped",
                        "Cannot auto-order item '" + item.getName()
                                + "' for location '" + loc.getName()
                                + "' because no enabled purchase option found."
                );
                continue;
            }

            Supplier sup = po.getSupplier();
            if (sup == null) {
                log.info("Purchase option has no supplier for item: {} - {}", item.getId(), item.getName());
                notificationService.createNotification(
                        companyId,
                        "Auto-Order Skipped",
                        "Cannot auto-order item '" + item.getName()
                                + "' for location '" + loc.getName()
                                + "' because purchase option has no supplier."
                );
                continue;
            }

            // record the shortage line
            ShortageLine sl = new ShortageLine(item.getId(), shortage, po.getPrice(), item.getName());
            shortageMap.computeIfAbsent(sup, k -> new ArrayList<>()).add(sl);
        }

        log.debug("Processed {} items, found {} with shortages across {} suppliers",
                itemsProcessed, shortagesFound, shortageMap.size());

        // 5) create or update draft for each supplier
        for (Map.Entry<Supplier, List<ShortageLine>> e : shortageMap.entrySet()) {
            Supplier sup = e.getKey();
            List<ShortageLine> lines = e.getValue();
            if (lines.isEmpty()) continue;

            // find draft
            Orders draft = purchaseOrderService.findDraftOrderForSupplierAndLocation(sup.getId(), loc.getId());
            if (draft == null) {
                // create new
                OrderCreateDTO dto = new OrderCreateDTO();
                dto.setBuyerLocationId(loc.getId());
                dto.setSupplierId(sup.getId());
                
                // Fetch system user ID
                Long systemUserId = userRepository.findByUsername("system-user")
                                        .map(Users::getId)
                                        .orElseThrow(() -> new RuntimeException("System user 'system-user' not found!"));
                dto.setCreatedByUserId(systemUserId); // Use fetched system user ID
                
                dto.setComments(
                        (setting.getAutoOrderComment() != null)
                                ? setting.getAutoOrderComment()
                                : "Auto-order by system"
                );

                List<OrderItemDTO> itemDtos = new ArrayList<>();
                for (ShortageLine sl : lines) {
                    OrderItemDTO idto = new OrderItemDTO();
                    idto.setInventoryItemId(sl.itemId());
                    idto.setQuantity(sl.shortageQty());
                    itemDtos.add(idto);
                }
                dto.setItems(itemDtos);

                log.info("Creating new draft order for supplier {} with {} items", sup.getName(), itemDtos.size());

                try {
                    Orders createdDraft = purchaseOrderService.createOrder(companyId, dto);
                    log.info("Successfully created draft order with ID: {}", createdDraft.getId());

                    // Show notification for newly created orders
                    notificationService.createNotification(
                            companyId,
                            "Auto-Order Created",
                            "Created new draft PO (ID=" + createdDraft.getId()
                                    + ") for supplier '" + sup.getName()
                                    + "', location '" + loc.getName() + "'"
                    );
                } catch (Exception ex) {
                    log.error("Error creating draft order: {}", ex.getMessage(), ex);

                    // Show notification for errors
                    notificationService.createNotification(
                            companyId,
                            "Auto-Order Error",
                            "Failed to create draft order for supplier '" + sup.getName()
                                    + "', location '" + loc.getName()
                                    + "'. Error: " + ex.getMessage()
                    );
                }
            } else {
                // update lines
                log.info("Updating existing draft order ID: {}", draft.getId());
                try {
                    Orders updatedDraft = purchaseOrderService.updateDraftOrderWithShortages(draft, lines, setting.getAutoOrderComment());

                    // Don't show notification for routine updates
                    // Only show notification if there was an actual change
                    if (updatedDraft != draft) {
                        log.debug("Order was updated with changes");
                    } else {
                        log.debug("No changes were made to the order");
                    }
                } catch (Exception ex) {
                    log.error("Error updating draft order: {}", ex.getMessage(), ex);

                    // Show notification for errors
                    notificationService.createNotification(
                            companyId,
                            "Auto-Order Error",
                            "Failed to update draft order (ID=" + draft.getId()
                                    + ") for supplier '" + sup.getName()
                                    + "', location '" + loc.getName()
                                    + "'. Error: " + ex.getMessage()
                    );
                }
            }
        }

        // Log the total execution time for performance monitoring
        Duration duration = Duration.between(startTime, LocalDateTime.now());
        log.info("Auto-order for location {} completed in {} ms", loc.getId(), duration.toMillis());
    }

    private PurchaseOption pickMainOrFallbackPurchaseOption(InventoryItem item) {
        if (item.getPurchaseOptions() == null || item.getPurchaseOptions().isEmpty()) return null;
        // find main
        Optional<PurchaseOption> mainOpt = item.getPurchaseOptions().stream()
                .filter(PurchaseOption::isMainPurchaseOption)
                .findFirst();
        if (mainOpt.isPresent()) return mainOpt.get();
        // else first
        return item.getPurchaseOptions().iterator().next();
    }

    private PurchaseOption pickEnabledMainOrFallbackPurchaseOption(InventoryItem item) {
        if (item.getPurchaseOptions() == null || item.getPurchaseOptions().isEmpty()) {
            return null;
        }
        // Filter only enabled
        List<PurchaseOption> enabledOptions = item.getPurchaseOptions().stream()
                .filter(PurchaseOption::isOrderingEnabled)
                .toList();
        if (enabledOptions.isEmpty()) {
            return null;
        }

        // find main
        Optional<PurchaseOption> mainOpt = enabledOptions.stream()
                .filter(PurchaseOption::isMainPurchaseOption)
                .findFirst();
        if (mainOpt.isPresent()) {
            return mainOpt.get();
        }
        // else the first enabled
        return enabledOptions.get(0);
    }

    // We'll define a small record for shortage lines
    public record ShortageLine(Long itemId, double shortageQty, Double price, String itemName) {}
}
