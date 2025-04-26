package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.ItemOrderInfoDTO;
import com.rayvision.inventory_management.model.dto.OrderCreateDTO;
import com.rayvision.inventory_management.model.dto.OrderItemDTO;
import com.rayvision.inventory_management.repository.*;
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
import java.util.stream.Collectors;

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
    private final SupplierRepository supplierRepository;

    public AutoOrderScheduledService(
            AutoOrderSettingRepository settingRepo,
            InventoryItemLocationService itemLocationService,
            PurchaseOrderService purchaseOrderService,
            NotificationService notificationService,
            LocationRepository locationRepository,
            InventoryItemRepository inventoryItemRepository,
            AssortmentLocationRepository assortmentLocationRepository,
            UserRepository userRepository,
            SupplierRepository supplierRepository
    ) {
        this.settingRepo = settingRepo;
        this.itemLocationService = itemLocationService;
        this.purchaseOrderService = purchaseOrderService;
        this.notificationService = notificationService;
        this.locationRepository = locationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.assortmentLocationRepository = assortmentLocationRepository;
        this.userRepository = userRepository;
        this.supplierRepository = supplierRepository;
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

    /**
     * Efficiently prepares order information DTOs for the given set of item IDs.
     * This loads all necessary data in a single query to avoid LazyInitializationException.
     */
    @Transactional(readOnly = true)
    public List<ItemOrderInfoDTO> prepareOrderInfoForItems(Collection<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<ItemOrderInfoDTO> result = new ArrayList<>();
        List<Object[]> queryResults = inventoryItemRepository.findAutoOrderDataByItemIds(itemIds);
        
        // Process query results into DTOs
        for (Object[] row : queryResults) {
            result.add(new ItemOrderInfoDTO(
                (Long) row[0],      // item.id
                (String) row[1],     // item.name
                (Long) row[2],      // po.id
                (Double) row[3],    // po.price
                (Long) row[4],      // supplier.id
                (String) row[5]      // supplier.name
            ));
        }
        
        return result;
    }
    
    /**
     * Efficiently prepares order information DTOs for all items in a company.
     * This loads all necessary data in a single query to avoid LazyInitializationException.
     */
    @Transactional(readOnly = true)
    public List<ItemOrderInfoDTO> prepareOrderInfoForCompany(Long companyId) {
        List<ItemOrderInfoDTO> result = new ArrayList<>();
        List<Object[]> queryResults = inventoryItemRepository.findAutoOrderDataByCompanyId(companyId);
        
        // Process query results into DTOs
        for (Object[] row : queryResults) {
            result.add(new ItemOrderInfoDTO(
                (Long) row[0],      // item.id
                (String) row[1],     // item.name
                (Long) row[2],      // po.id
                (Double) row[3],    // po.price
                (Long) row[4],      // supplier.id
                (String) row[5]      // supplier.name
            ));
        }
        
        return result;
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

        // Re-fetch company to avoid LazyInitialization
        Company company = loc.getCompany();
        if (company == null) {
            log.warn("Company is null for location ID: {}", loc.getId());
            return;
        }
        
        Long companyId = company.getId();
        
        // Get assortments for this location
        List<AssortmentLocation> bridgingAssortments = 
                assortmentLocationRepository.findByLocationId(loc.getId());

        // Prepare collection of item IDs we need to process
        Set<Long> itemIds = new HashSet<>();
        
        // If there are assortments, collect all item IDs from them
        if (!bridgingAssortments.isEmpty()) {
            for (AssortmentLocation al : bridgingAssortments) {
                Assortment assortment = al.getAssortment();
                if (assortment != null && assortment.getInventoryItems() != null) {
                    for (InventoryItem item : assortment.getInventoryItems()) {
                        itemIds.add(item.getId());
                    }
                }
            }
            log.debug("Found {} items in assortments for location ID: {}", itemIds.size(), loc.getId());
        }
        
        // Load order information using the efficient DTO-based approach
        List<ItemOrderInfoDTO> orderInfoItems;
        if (bridgingAssortments.isEmpty()) {
            // If no assortments, load data for all company items
            orderInfoItems = prepareOrderInfoForCompany(companyId);
            log.debug("No assortments found, loaded {} items with purchase options", orderInfoItems.size());
        } else {
            // If we have assortments, load data only for those items
            orderInfoItems = prepareOrderInfoForItems(itemIds);
            log.debug("Loaded {} items with purchase options from assortments", orderInfoItems.size());
        }
        
        // Create a map from item ID to order info for quick lookup
        Map<Long, List<ItemOrderInfoDTO>> itemOrderInfoMap = orderInfoItems.stream()
                .collect(Collectors.groupingBy(ItemOrderInfoDTO::itemId));
        
        // Get itemLocation bridging rows
        List<InventoryItemLocation> bridgingList = itemLocationService.getByLocation(loc.getId());
        if (bridgingList.isEmpty()) {
            log.warn("No inventory item location bridging rows found for location: {}", loc.getId());
            return; // no items
        }

        // Build a map itemId -> bridging row
        Map<Long, InventoryItemLocation> iilMap = new HashMap<>();
        for (InventoryItemLocation bil : bridgingList) {
            iilMap.put(bil.getInventoryItem().getId(), bil);
        }

        // Get in-transit quantities from sent orders
        Map<Long, Double> inTransitMap = purchaseOrderService
                .calculateInTransitQuantitiesByLocation(loc.getId());
        log.debug("Found {} items with in-transit quantities", inTransitMap.size());

        // For each item, compute shortage if par > 0
        // We organize shortages by supplier ID (using the efficient DTO approach)
        Map<Long, List<ShortageInfo>> shortagesBySupplier = new HashMap<>();
        int itemsProcessed = 0;
        int shortagesFound = 0;

        // Process each item that has inventory location data
        for (Long itemId : itemOrderInfoMap.keySet()) {
            itemsProcessed++;
            InventoryItemLocation bil = iilMap.get(itemId);
            if (bil == null) {
                // No bridging row => skip
                continue;
            }

            // Skip items with both minOnHand & par=0
            double min = (bil.getMinOnHand() != null) ? bil.getMinOnHand() : 0.0;
            double par = (bil.getParLevel() != null) ? bil.getParLevel() : 0.0;
            if ((min <= 0) && (par <= 0)) {
                // User doesn't want auto-order
                continue;
            }

            // Compute shortage from par - onHand
            double onHand = (bil.getOnHand() != null) ? bil.getOnHand() : 0.0;
            double inTransitQty = inTransitMap.getOrDefault(itemId, 0.0);
            double effectiveOnHand = onHand + inTransitQty;
            double shortage = par - effectiveOnHand;

            if (shortage <= 0) {
                // No ordering needed
                continue;
            }

            // Find the best purchase option using our DTO data
            List<ItemOrderInfoDTO> options = itemOrderInfoMap.get(itemId);
            if (options == null || options.isEmpty()) {
                log.info("No purchase options found for item ID: {}", itemId);
                notificationService.createNotification(
                        companyId,
                        "Auto-Order Skipped",
                        "Cannot auto-order item '" + bil.getInventoryItem().getName()
                                + "' for location '" + loc.getName()
                                + "' because no enabled purchase option found."
                );
                continue;
            }

            // Find the main purchase option or the first one
            ItemOrderInfoDTO selectedOption = findBestPurchaseOption(options);
            if (selectedOption == null) {
                continue;
            }

            shortagesFound++;
            log.debug("Item {} - {} has shortage of {} (Par: {}, OnHand: {}, In-transit: {}, Min: {})",
                    itemId, selectedOption.itemName(), shortage, par, onHand, inTransitQty, min);

            // Add to our shortage map by supplier ID
            ShortageInfo shortageInfo = new ShortageInfo(
                    itemId, 
                    selectedOption.itemName(),
                    shortage, 
                    selectedOption.price()
            );
            
            shortagesBySupplier.computeIfAbsent(selectedOption.supplierId(), k -> new ArrayList<>())
                    .add(shortageInfo);
        }

        log.debug("Processed {} items, found {} with shortages across {} suppliers",
                itemsProcessed, shortagesFound, shortagesBySupplier.size());

        // Create or update draft order for each supplier
        for (Map.Entry<Long, List<ShortageInfo>> entry : shortagesBySupplier.entrySet()) {
            Long supplierId = entry.getKey();
            List<ShortageInfo> shortages = entry.getValue();
            if (shortages.isEmpty()) continue;

            // Find the supplier name from any of the shortage items
            String supplierName = orderInfoItems.stream()
                    .filter(info -> info.supplierId().equals(supplierId))
                    .findFirst()
                    .map(ItemOrderInfoDTO::supplierName)
                    .orElse("Unknown Supplier");

            // Find draft order
            Orders draft = purchaseOrderService.findDraftOrderForSupplierAndLocation(supplierId, loc.getId());
            if (draft == null) {
                // Create new draft order
                createDraftOrder(setting, companyId, loc, supplierId, supplierName, shortages);
            } else {
                // Update existing draft order
                updateDraftOrder(setting, companyId, loc.getName(), supplierName, draft, shortages);
            }
        }

        // Log the total execution time for performance monitoring
        Duration duration = Duration.between(startTime, LocalDateTime.now());
        log.info("Auto-order for location {} completed in {} ms", loc.getId(), duration.toMillis());
    }
    
    private ItemOrderInfoDTO findBestPurchaseOption(List<ItemOrderInfoDTO> options) {
        // First try to find a "main" purchase option (the one set as default)
        // Since we can't filter by mainPurchaseOption in our DTO query, we'll use the first option
        // This is a limitation of the DTO approach, but in practice it's often acceptable
        
        if (!options.isEmpty()) {
            return options.get(0); // Take the first one which will be enabled due to our query
        }
        return null;
    }
    
    @Transactional
    public void createDraftOrder(
            AutoOrderSetting setting, 
            Long companyId, 
            Location loc, 
            Long supplierId,
            String supplierName,
            List<ShortageInfo> shortages
    ) {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setBuyerLocationId(loc.getId());
        dto.setSupplierId(supplierId);
        
        // Fetch system user ID
        Long systemUserId = userRepository.findByUsername("system-user")
                .map(Users::getId)
                .orElseThrow(() -> new RuntimeException("System user 'system-user' not found!"));
        dto.setCreatedByUserId(systemUserId);
        
        dto.setComments(
                (setting.getAutoOrderComment() != null)
                        ? setting.getAutoOrderComment()
                        : "Auto-order by system"
        );

        List<OrderItemDTO> itemDtos = new ArrayList<>();
        for (ShortageInfo shortage : shortages) {
            OrderItemDTO idto = new OrderItemDTO();
            idto.setInventoryItemId(shortage.itemId);
            idto.setQuantity(shortage.shortageQty);
            itemDtos.add(idto);
        }
        dto.setItems(itemDtos);

        log.info("Creating new draft order for supplier {} with {} items", supplierName, itemDtos.size());

        try {
            Orders createdDraft = purchaseOrderService.createOrder(companyId, dto);
            log.info("Successfully created draft order with ID: {}", createdDraft.getId());

            // Show notification for newly created orders
            notificationService.createNotification(
                    companyId,
                    "Auto-Order Created",
                    "Created new draft PO (ID=" + createdDraft.getId()
                            + ") for supplier '" + supplierName
                            + "', location '" + loc.getName() + "'"
            );
        } catch (Exception ex) {
            log.error("Error creating draft order: {}", ex.getMessage(), ex);

            // Show notification for errors
            notificationService.createNotification(
                    companyId,
                    "Auto-Order Error",
                    "Failed to create draft order for supplier '" + supplierName
                            + "', location '" + loc.getName()
                            + "'. Error: " + ex.getMessage()
            );
        }
    }
    
    @Transactional
    public void updateDraftOrder(
            AutoOrderSetting setting,
            Long companyId,
            String locationName,
            String supplierName,
            Orders draft,
            List<ShortageInfo> shortages
    ) {
        log.info("Updating existing draft order ID: {}", draft.getId());
        try {
            // Convert our ShortageInfo objects to what purchaseOrderService expects (ShortageLine)
            List<ShortageLine> shortageLines = shortages.stream()
                    .map(s -> new ShortageLine(s.itemId, s.shortageQty, s.price, s.itemName))
                    .collect(Collectors.toList());
                    
            Orders updatedDraft = purchaseOrderService.updateDraftOrderWithShortages(
                    draft, 
                    shortageLines, 
                    setting.getAutoOrderComment()
            );

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
                            + ") for supplier '" + supplierName
                            + "', location '" + locationName
                            + "'. Error: " + ex.getMessage()
            );
        }
    }

    // ShortageInfo class to hold shortage information internally
    private static class ShortageInfo {
        final Long itemId;
        final String itemName;
        final double shortageQty;
        final Double price;
        
        ShortageInfo(Long itemId, String itemName, double shortageQty, Double price) {
            this.itemId = itemId;
            this.itemName = itemName;
            this.shortageQty = shortageQty;
            this.price = price;
        }
    }

    // We'll define a small record for shortage lines for compatibility with existing code
    public record ShortageLine(Long itemId, double shortageQty, Double price, String itemName) {}
}
