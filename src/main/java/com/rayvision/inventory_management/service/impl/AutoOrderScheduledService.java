package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.ItemOrderInfoDTO;
import com.rayvision.inventory_management.model.dto.OrderCreateDTO;
import com.rayvision.inventory_management.model.dto.OrderItemDTO;
import com.rayvision.inventory_management.repository.*;
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
    private final InventoryItemRepository inventoryItemRepository;
    private final AssortmentLocationRepository assortmentLocationRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final NotificationService notificationService;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public AutoOrderScheduledService(
            AutoOrderSettingRepository settingRepo,
            InventoryItemRepository inventoryItemRepository,
            AssortmentLocationRepository assortmentLocationRepository,
            PurchaseOrderService purchaseOrderService,
            NotificationService notificationService,
            LocationRepository locationRepository,
            UserRepository userRepository,
            OrderRepository orderRepository
    ) {
        this.settingRepo = settingRepo;
        this.inventoryItemRepository = inventoryItemRepository;
        this.assortmentLocationRepository = assortmentLocationRepository;
        this.purchaseOrderService = purchaseOrderService;
        this.notificationService = notificationService;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
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
     * Efficiently loads all auto-order data for a specific company and location.
     * This comprehensive query loads items, purchase options, supplier info, and inventory data
     * in a single database call.
     */
    @Transactional(readOnly = true)
    public List<ItemOrderInfoDTO> loadCompleteAutoOrderData(Long locationId, Long companyId) {
        List<ItemOrderInfoDTO> result = new ArrayList<>();
        
        // Step 1: Get all relevant item data in a single query including inventory location data
        List<Object[]> rawData = inventoryItemRepository.findCompleteAutoOrderDataByLocationAndCompany(locationId, companyId);
        
        // Step 2: Get in-transit quantities in a single query
        Map<Long, Double> inTransitMap = new HashMap<>();
        List<Object[]> inTransitData = orderRepository.getInTransitQuantitiesByLocation(locationId);
        for (Object[] row : inTransitData) {
            Long itemId = (Long) row[0];
            Double quantity = (Double) row[1];
            inTransitMap.put(itemId, quantity);
        }
        
        // Step 3: Process the raw data into DTOs with in-transit quantities
        for (Object[] row : rawData) {
            Long itemId = (Long) row[0];
            String itemName = (String) row[1];
            
            Long purchaseOptionId = (Long) row[2];
            Double price = (Double) row[3];
            Boolean isMainOption = (Boolean) row[4];
            
            Long supplierId = (Long) row[5];
            String supplierName = (String) row[6];
            
            Double onHand = (Double) row[7];
            Double minOnHand = (Double) row[8];
            Double parLevel = (Double) row[9];
            
            // Get in-transit quantity from our map, defaulting to 0.0 if not found
            Double inTransitQty = inTransitMap.getOrDefault(itemId, 0.0);
            
            result.add(new ItemOrderInfoDTO(
                itemId, itemName,
                purchaseOptionId, price, isMainOption,
                supplierId, supplierName,
                onHand, minOnHand, parLevel,
                inTransitQty
            ));
        }
        
        return result;
    }
    
    /**
     * Similar to loadCompleteAutoOrderData but filters by specific item IDs from assortments.
     */
    @Transactional(readOnly = true)
    public List<ItemOrderInfoDTO> loadCompleteAutoOrderDataForItemIds(Collection<Long> itemIds, Long locationId) {
        if (itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<ItemOrderInfoDTO> result = new ArrayList<>();
        
        // Step 1: Get item data including inventory location info
        List<Object[]> rawData = inventoryItemRepository.findCompleteAutoOrderDataByItemIdsAndLocation(itemIds, locationId);
        
        // Step 2: Get in-transit quantities
        Map<Long, Double> inTransitMap = new HashMap<>();
        List<Object[]> inTransitData = orderRepository.getInTransitQuantitiesByLocation(locationId);
        for (Object[] row : inTransitData) {
            Long itemId = (Long) row[0];
            Double quantity = (Double) row[1];
            if (itemIds.contains(itemId)) { // Only include items in our filter list
                inTransitMap.put(itemId, quantity);
            }
        }
        
        // Step 3: Process into DTOs
        for (Object[] row : rawData) {
            Long itemId = (Long) row[0];
            String itemName = (String) row[1];
            
            Long purchaseOptionId = (Long) row[2];
            Double price = (Double) row[3];
            Boolean isMainOption = (Boolean) row[4];
            
            Long supplierId = (Long) row[5];
            String supplierName = (String) row[6];
            
            Double onHand = (Double) row[7];
            Double minOnHand = (Double) row[8];
            Double parLevel = (Double) row[9];
            
            Double inTransitQty = inTransitMap.getOrDefault(itemId, 0.0);
            
            result.add(new ItemOrderInfoDTO(
                itemId, itemName,
                purchaseOptionId, price, isMainOption,
                supplierId, supplierName,
                onHand, minOnHand, parLevel,
                inTransitQty
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
        Long locationId = loc.getId();
        
        // Get assortments for this location to determine which items to check
        List<AssortmentLocation> bridgingAssortments = 
                assortmentLocationRepository.findByLocationId(locationId);

        List<ItemOrderInfoDTO> orderInfoItems;
        
        if (bridgingAssortments.isEmpty()) {
            // If no assortments, process all company items
            orderInfoItems = loadCompleteAutoOrderData(locationId, companyId);
            log.debug("No assortments found, loaded {} items with purchase options and inventory data", 
                      orderInfoItems.size());
        } else {
            // If we have assortments, collect item IDs and only process those
            Set<Long> itemIds = new HashSet<>();
            for (AssortmentLocation al : bridgingAssortments) {
                Assortment assortment = al.getAssortment();
                if (assortment != null && assortment.getInventoryItems() != null) {
                    for (InventoryItem item : assortment.getInventoryItems()) {
                        itemIds.add(item.getId());
                    }
                }
            }
            
            if (itemIds.isEmpty()) {
                log.warn("No items found in assortments for location ID: {}", locationId);
                return;
            }
            
            orderInfoItems = loadCompleteAutoOrderDataForItemIds(itemIds, locationId);
            log.debug("Loaded {} items with purchase options and inventory data from assortments", 
                      orderInfoItems.size());
        }

        // Filter to only include items that need ordering
        List<ItemOrderInfoDTO> itemsToOrder = orderInfoItems.stream()
                .filter(ItemOrderInfoDTO::needsOrdering)
                .toList();
        
        log.debug("Found {} items that need ordering out of {} total items", 
                 itemsToOrder.size(), orderInfoItems.size());
        
        // Group by supplier for order creation
        Map<Long, List<ItemOrderInfoDTO>> itemsBySupplier = itemsToOrder.stream()
                .collect(Collectors.groupingBy(ItemOrderInfoDTO::supplierId));
                
        // Create or update orders for each supplier
        for (Map.Entry<Long, List<ItemOrderInfoDTO>> entry : itemsBySupplier.entrySet()) {
            Long supplierId = entry.getKey();
            List<ItemOrderInfoDTO> items = entry.getValue();
            
            if (items.isEmpty()) continue;
            
            String supplierName = items.get(0).supplierName();
            
            // Find draft order for this supplier/location
            Orders draft = purchaseOrderService.findDraftOrderForSupplierAndLocation(supplierId, locationId);
            
            if (draft == null) {
                // Create new draft order
                createDraftOrder(setting, companyId, loc, supplierId, supplierName, items);
            } else {
                // Update existing draft order
                updateDraftOrder(setting, companyId, loc.getName(), supplierName, draft, items);
            }
        }

        // Log the total execution time for performance monitoring
        Duration duration = Duration.between(startTime, LocalDateTime.now());
        log.info("Auto-order for location {} completed in {} ms", locationId, duration.toMillis());
    }
    
    @Transactional
    public void createDraftOrder(
            AutoOrderSetting setting, 
            Long companyId, 
            Location loc, 
            Long supplierId, 
            String supplierName,
            List<ItemOrderInfoDTO> items
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
        for (ItemOrderInfoDTO item : items) {
            OrderItemDTO idto = new OrderItemDTO();
            idto.setInventoryItemId(item.itemId());
            idto.setQuantity(item.calculateShortage());
            itemDtos.add(idto);
        }
        dto.setItems(itemDtos);

        log.info("Creating new draft order for supplier {} with {} items", supplierName, itemDtos.size());

        try {
            Orders createdDraft = purchaseOrderService.createOrder(companyId, dto);
            log.info("Successfully created draft order with ID: {}", createdDraft.getId());

            notificationService.createNotification(
                    companyId,
                    "Auto-Order Created",
                    "Created new draft PO (ID=" + createdDraft.getId()
                            + ") for supplier '" + supplierName
                            + "', location '" + loc.getName() + "'"
            );
        } catch (Exception ex) {
            log.error("Error creating draft order: {}", ex.getMessage(), ex);

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
            List<ItemOrderInfoDTO> items
    ) {
        log.info("Updating existing draft order ID: {}", draft.getId());
        try {
            // Convert ItemOrderInfoDTO objects to what purchaseOrderService expects (ShortageLine)
            List<ShortageLine> shortageLines = items.stream()
                    .map(item -> new ShortageLine(
                            item.itemId(), 
                            item.calculateShortage(), 
                            item.price(), 
                            item.itemName()
                    ))
                    .collect(Collectors.toList());
            
            Orders updatedDraft = purchaseOrderService.updateDraftOrderWithShortages(
                    draft, 
                    shortageLines, 
                    setting.getAutoOrderComment()
            );

            if (updatedDraft != draft) {
                log.debug("Order was updated with changes");
            } else {
                log.debug("No changes were made to the order");
            }
        } catch (Exception ex) {
            log.error("Error updating draft order: {}", ex.getMessage(), ex);

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

    // We'll define a small record for shortage lines for compatibility with existing code
    public record ShortageLine(Long itemId, double shortageQty, Double price, String itemName) {}
}
