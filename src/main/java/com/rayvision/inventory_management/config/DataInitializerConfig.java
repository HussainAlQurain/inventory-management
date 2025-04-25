package com.rayvision.inventory_management.config;

import com.rayvision.inventory_management.enums.SubRecipeType;
import com.rayvision.inventory_management.enums.UoMCategory;
import com.rayvision.inventory_management.enums.userRoles;
import com.rayvision.inventory_management.model.*;
import com.rayvision.inventory_management.model.dto.MenuItemCreateDTO;
import com.rayvision.inventory_management.model.dto.MenuItemLineDTO;
import com.rayvision.inventory_management.model.dto.SubRecipeCreateDTO;
import com.rayvision.inventory_management.model.dto.SubRecipeLineDTO;
import com.rayvision.inventory_management.repository.*;
import com.rayvision.inventory_management.service.CategoryService;
import com.rayvision.inventory_management.service.MenuItemService;
import com.rayvision.inventory_management.service.SubRecipeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Configuration
@Profile("!test") // Don't run during tests
public class DataInitializerConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializerConfig.class);
    private static final int NUM_SUPPLIERS = 1000;
    private static final int NUM_LOCATIONS = 200;
    private static final int NUM_USERS = 199; // Start from user2
    private static final int NUM_INVENTORY_ITEMS = 5000; // Reduced from 10k for faster init
    private static final int NUM_SUB_RECIPES = 1000; // Reduced from 2k
    private static final int NUM_MENU_ITEMS = 1000; // Reduced from 2k
    private static final Long COMPANY_ID = 1L;
    private static final String DEFAULT_EMAIL = "hussain.qurain@outlook.com";
    private static final String DEFAULT_PHONE = "0555555555";
    private static final String DEFAULT_KSA_CITY_1 = "Khobar";
    private static final String DEFAULT_KSA_CITY_2 = "Dammam";
    private static final String DEFAULT_KSA_STATE = "Eastern Province";

    private final CompanyRepository companyRepository;
    private final LocationRepository locationRepository;
    private final CategoryService categoryService; // Use service for categories
    private final CategoryRepository categoryRepository; // Also need repo for check
    private final UnitOfMeasureRepository unitOfMeasureRepository;
    private final UnitOfMeasureCategoryRepository unitOfMeasureCategoryRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CompanyUserRepository companyUserRepository;
    private final LocationUserRepository locationUserRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PurchaseOptionRepository purchaseOptionRepository;
    private final SubRecipeService subRecipeService; // Use service for subrecipes
    private final MenuItemService menuItemService; // Use service for menu items
    private final PasswordEncoder passwordEncoder;
    private final Random random = ThreadLocalRandom.current();


    public DataInitializerConfig(CompanyRepository companyRepository,
                                 LocationRepository locationRepository,
                                 CategoryService categoryService,
                                 CategoryRepository categoryRepository,
                                 UnitOfMeasureRepository unitOfMeasureRepository,
                                 UnitOfMeasureCategoryRepository unitOfMeasureCategoryRepository,
                                 SupplierRepository supplierRepository,
                                 UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 CompanyUserRepository companyUserRepository,
                                 LocationUserRepository locationUserRepository,
                                 InventoryItemRepository inventoryItemRepository,
                                 PurchaseOptionRepository purchaseOptionRepository,
                                 SubRecipeService subRecipeService,
                                 MenuItemService menuItemService,
                                 PasswordEncoder passwordEncoder) {
        this.companyRepository = companyRepository;
        this.locationRepository = locationRepository;
        this.categoryService = categoryService;
        this.categoryRepository = categoryRepository;
        this.unitOfMeasureRepository = unitOfMeasureRepository;
        this.unitOfMeasureCategoryRepository = unitOfMeasureCategoryRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.companyUserRepository = companyUserRepository;
        this.locationUserRepository = locationUserRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.purchaseOptionRepository = purchaseOptionRepository;
        this.subRecipeService = subRecipeService;
        this.menuItemService = menuItemService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional // Use transaction for atomicity
    public void run(String... args) throws Exception {
        log.info("Checking if data initialization is needed...");

        // Check if a specific category exists for Company 1 to prevent re-initialization
        boolean alreadyInitialized = categoryRepository.findByCompanyId(COMPANY_ID)
                .stream()
                .anyMatch(cat -> "Food".equals(cat.getName()));

        if (alreadyInitialized) {
            log.info("Data initialization already performed or core data exists. Skipping.");
            return;
        }

        log.info("Starting data initialization...");

        try {
            // 1. Fetch essential existing data
            Company company1 = companyRepository.findById(COMPANY_ID)
                    .orElseThrow(() -> new RuntimeException("Company with ID " + COMPANY_ID + " not found. Ensure data.sql ran correctly."));

            Map<userRoles, Role> rolesMap = fetchRoles();

            // 2. Create Core Data
            Map<UoMCategory, UnitOfMeasureCategory> uomCategories = createUnitOfMeasureCategories(company1);
            List<UnitOfMeasure> uoms = createUnitsOfMeasure(company1, uomCategories);
            List<Category> categories = createCategories(company1);

            // Group UOMs by Category ID for efficient lookup
            Map<Long, List<UnitOfMeasure>> uomsByCategory = uoms.stream()
                    .filter(uom -> uom.getCategory() != null)
                    .collect(Collectors.groupingBy(uom -> uom.getCategory().getId()));

            // 3. Create Bulk Data
            List<Supplier> suppliers = createSuppliers(company1, categories);
            List<Location> locations = createLocations(company1); // Create locations after company
            createUsers(company1, locations, rolesMap); // Create users after locations and roles
            List<InventoryItem> inventoryItems = createInventoryItems(company1, categories, uoms, suppliers); // Pass uomsByCategory map
            List<SubRecipe> subRecipes = createSubRecipes(company1, categories, uoms, inventoryItems, uomsByCategory); // Pass uomsByCategory map
            createMenuItems(company1, categories, uoms, inventoryItems, subRecipes, uomsByCategory); // Pass uomsByCategory map

            log.info("Data initialization completed successfully.");

        } catch (Exception e) {
            log.error("Error during data initialization: {}", e.getMessage(), e);
            throw e;
        }
    }

    private Map<userRoles, Role> fetchRoles() {
        Map<userRoles, Role> rolesMap = new EnumMap<>(userRoles.class);
        List<userRoles> requiredRoleEnums = List.of(userRoles.ROLE_USER, userRoles.ROLE_STAFF, userRoles.ROLE_MANAGER, userRoles.ROLE_ADMIN);
        for (userRoles roleEnum : requiredRoleEnums) {
            Role role = roleRepository.findByName(roleEnum);
            if (role == null) {
                throw new RuntimeException("Role " + roleEnum.name() + " not found in database. Ensure data.sql ran correctly.");
            }
            rolesMap.put(roleEnum, role);
        }
        log.info("Fetched {} roles.", rolesMap.size());
        return rolesMap;
    }

    private Map<UoMCategory, UnitOfMeasureCategory> createUnitOfMeasureCategories(Company company) {
        log.info("Creating Unit of Measure Categories...");
        Map<UoMCategory, UnitOfMeasureCategory> categories = new EnumMap<>(UoMCategory.class);

        for (UoMCategory categoryEnum : UoMCategory.values()) {
            UnitOfMeasureCategory uomCategory = UnitOfMeasureCategory.builder()
                    .name(categoryEnum.name())
                    .description("Category for " + categoryEnum.name())
                    .company(company)
                    .build();
            categories.put(categoryEnum, unitOfMeasureCategoryRepository.save(uomCategory));
        }

        log.info("Created {} Unit of Measure Categories.", categories.size());
        return categories;
    }

    private List<UnitOfMeasure> createUnitsOfMeasure(Company company, Map<UoMCategory, UnitOfMeasureCategory> uomCategories) {
        log.info("Creating Units of Measure...");

        // Fetch existing abbreviations for this company to avoid duplicates
        Set<String> existingAbbreviations = unitOfMeasureRepository.findByCompanyId(company.getId())
                .stream()
                .map(UnitOfMeasure::getAbbreviation)
                .collect(Collectors.toSet());
        log.info("Found {} existing UOM abbreviations for company {}.", existingAbbreviations.size(), company.getId());

        List<UnitOfMeasure> uomsToAdd = new ArrayList<>();
        List<UnitOfMeasure> allUoms = new ArrayList<>(unitOfMeasureRepository.findByCompanyId(company.getId())); // Start with existing

        // Define desired UOMs
        Map<String, UnitOfMeasure.UnitOfMeasureBuilder> desiredUoms = new LinkedHashMap<>(); // Use LinkedHashMap to maintain order

        // MASS
        UnitOfMeasureCategory massCategory = uomCategories.get(UoMCategory.MASS);
        desiredUoms.put("kg", UnitOfMeasure.builder().name("Kilogram").abbreviation("kg").category(massCategory).conversionFactor(1000.0).company(company));
        desiredUoms.put("g", UnitOfMeasure.builder().name("Gram").abbreviation("g").category(massCategory).conversionFactor(1.0).company(company)); // Base for MASS
        desiredUoms.put("lb", UnitOfMeasure.builder().name("Pound").abbreviation("lb").category(massCategory).conversionFactor(453.592).company(company));
        desiredUoms.put("oz", UnitOfMeasure.builder().name("Ounce").abbreviation("oz").category(massCategory).conversionFactor(28.3495).company(company));
        // VOLUME
        UnitOfMeasureCategory volumeCategory = uomCategories.get(UoMCategory.VOLUME);
        desiredUoms.put("L", UnitOfMeasure.builder().name("Liter").abbreviation("L").category(volumeCategory).conversionFactor(1000.0).company(company));
        desiredUoms.put("ml", UnitOfMeasure.builder().name("Milliliter").abbreviation("ml").category(volumeCategory).conversionFactor(1.0).company(company)); // Base for VOLUME
        desiredUoms.put("gal", UnitOfMeasure.builder().name("Gallon").abbreviation("gal").category(volumeCategory).conversionFactor(3785.41).company(company));
        desiredUoms.put("fl oz", UnitOfMeasure.builder().name("Fluid Ounce").abbreviation("fl oz").category(volumeCategory).conversionFactor(29.5735).company(company));
        // COUNT
        UnitOfMeasureCategory countCategory = uomCategories.get(UoMCategory.COUNT);
        desiredUoms.put("ea", UnitOfMeasure.builder().name("Each").abbreviation("ea").category(countCategory).conversionFactor(1.0).company(company)); // Base for COUNT
        desiredUoms.put("dz", UnitOfMeasure.builder().name("Dozen").abbreviation("dz").category(countCategory).conversionFactor(12.0).company(company));
        desiredUoms.put("cs", UnitOfMeasure.builder().name("Case").abbreviation("cs").category(countCategory).conversionFactor(1.0).company(company)); // Factor depends on item

        // Check each desired UOM and add if abbreviation doesn't exist
        desiredUoms.forEach((abbr, builder) -> {
            if (!existingAbbreviations.contains(abbr)) {
                log.debug("Adding UOM with abbreviation: {}", abbr);
                uomsToAdd.add(builder.build());
            } else {
                log.debug("Skipping UOM with existing abbreviation: {}", abbr);
            }
        });

        // Save only the new UOMs
        if (!uomsToAdd.isEmpty()) {
            List<UnitOfMeasure> savedNewUoms = unitOfMeasureRepository.saveAll(uomsToAdd);
            allUoms.addAll(savedNewUoms);
            log.info("Created {} new Units of Measure.", savedNewUoms.size());
        } else {
            log.info("No new Units of Measure to create.");
        }

        // Return all UOMs (existing + newly created) for the company
        return allUoms;
    }

    private List<Category> createCategories(Company company) {
        log.info("Creating Categories...");
        List<Category> categories = new ArrayList<>();
        List<String> categoryNames = List.of("Food", "Beverages", "Cleaning", "Supplies", "Other");
        for (String name : categoryNames) {
            Category cat = new Category();
            cat.setName(name);
            cat.setDescription("Category for " + name);
            categories.add(categoryService.save(company.getId(), cat));
        }
        log.info("Created {} Categories.", categories.size());
        return categories;
    }

    private List<Supplier> createSuppliers(Company company, List<Category> categories) {
        log.info("Creating {} Suppliers...", NUM_SUPPLIERS);
        List<Supplier> suppliers = new ArrayList<>();
        for (int i = 1; i <= NUM_SUPPLIERS; i++) {
            Supplier supplier = new Supplier();
            supplier.setName("Supplier " + i);
            supplier.setCustomerNumber(String.valueOf(1000 + i));
            supplier.setTaxId(String.valueOf(3000000000000L + i));
            supplier.setTaxRate(15.0);
            supplier.setMinimumOrder(random.nextDouble(50.0, 500.0));
            supplier.setAddress("123 Supplier St");
            supplier.setCity(i % 2 == 0 ? DEFAULT_KSA_CITY_1 : DEFAULT_KSA_CITY_2);
            supplier.setState(DEFAULT_KSA_STATE);
            supplier.setZip("31952");
            supplier.setDefaultCategory(getRandomElement(categories, random));
            supplier.setCompany(company);

            SupplierEmail email = new SupplierEmail();
            email.setEmail(DEFAULT_EMAIL);
            email.setSupplier(supplier);
            supplier.getOrderEmails().add(email);

            SupplierPhone phone = new SupplierPhone();
            phone.setPhoneNumber(DEFAULT_PHONE);
            phone.setSupplier(supplier);
            supplier.getOrderPhones().add(phone);

            suppliers.add(supplier);
        }
        List<Supplier> savedSuppliers = supplierRepository.saveAll(suppliers);
        log.info("Created {} Suppliers.", savedSuppliers.size());
        return savedSuppliers;
    }

    private List<Location> createLocations(Company company) {
        log.info("Creating {} Locations...", NUM_LOCATIONS);
        List<Location> locationsToSave = new ArrayList<>();
        List<Location> existingLocations = locationRepository.findByCompanyId(company.getId());
        Set<String> existingLocationNames = existingLocations.stream()
                .map(Location::getName)
                .collect(Collectors.toSet());

        boolean defaultLocationExists = existingLocationNames.contains("Default Location");

        for (int i = 1; i <= NUM_LOCATIONS; i++) {
            String locationName = "Location " + i;

            if (locationName.equals("Default Location") && defaultLocationExists) {
                log.warn("Skipping creation of 'Default Location' as it already exists.");
                continue;
            }
            if (existingLocationNames.contains(locationName)) {
                log.warn("Skipping creation of '{}' as it already exists.", locationName);
                continue;
            }

            Location location = new Location();
            location.setName(locationName);
            location.setCode("LOC" + i);
            location.setAddress("Street " + i);
            location.setCity(i % 2 == 0 ? DEFAULT_KSA_CITY_1 : DEFAULT_KSA_CITY_2);
            location.setState(DEFAULT_KSA_STATE);
            location.setZip(String.valueOf(31900 + i));
            location.setPhone(DEFAULT_PHONE);
            location.setCompany(company);
            locationsToSave.add(location);
        }

        List<Location> savedNewLocations = locationRepository.saveAll(locationsToSave);
        log.info("Created {} new Locations.", savedNewLocations.size());

        List<Location> allCompanyLocations = locationRepository.findByCompanyId(company.getId());
        log.info("Total locations for company {}: {}", company.getId(), allCompanyLocations.size());
        return allCompanyLocations;
    }

    private void createUsers(Company company, List<Location> locations, Map<userRoles, Role> rolesMap) {
        log.info("Creating {} Users...", NUM_USERS);
        if (locations.isEmpty()) {
            log.error("No locations available to assign users to. Skipping user creation.");
            return;
        }

        // Fetch existing usernames to avoid redundant checks inside the loop
        Set<String> existingUsernames = userRepository.findAll().stream()
                .map(Users::getUsername)
                .collect(Collectors.toSet());
        log.debug("Found {} existing usernames.", existingUsernames.size());

        List<userRoles> assignableRoles = List.of(userRoles.ROLE_USER, userRoles.ROLE_STAFF, userRoles.ROLE_MANAGER, userRoles.ROLE_ADMIN);
        List<Users> usersToSave = new ArrayList<>();
        List<CompanyUser> companyUsersToSave = new ArrayList<>();
        List<LocationUser> locationUsersToSave = new ArrayList<>();

        for (int i = 2; i <= NUM_USERS + 1; i++) {
            String username = "user" + i;
            if (existingUsernames.contains(username)) {
                log.warn("User '{}' already exists. Skipping creation.", username);
                continue;
            }

            Users user = new Users();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(username)); // Encode password
            user.setEmail(username + "@example.com");
            user.setFirstName("User");
            user.setLastName(String.valueOf(i));
            user.setPhone(DEFAULT_PHONE);
            user.setStatus("active");
            user.setRoles(new HashSet<>()); // Initialize roles set

            userRoles roleEnum = getRandomElement(assignableRoles, random);
            Role role = rolesMap.get(roleEnum);
            if (role != null) {
                user.getRoles().add(role);
            } else {
                log.warn("Role {} not found for user {}", roleEnum, user.getUsername());
            }

            usersToSave.add(user); // Add user to list for batch save
        }

        // Batch save users
        List<Users> savedUsers = userRepository.saveAll(usersToSave);
        log.info("Saved {} new Users.", savedUsers.size());

        // Create associations for saved users
        for (Users savedUser : savedUsers) {
            // Company association
            CompanyUser cu = new CompanyUser();
            cu.setUser(savedUser);
            cu.setCompany(company);
            companyUsersToSave.add(cu);

            // Location association (randomly assigned)
            Location randomLocation = getRandomElement(locations, random);
            LocationUser lu = new LocationUser();
            lu.setUser(savedUser);
            lu.setLocation(randomLocation);
            locationUsersToSave.add(lu);
        }

        // Batch save associations
        companyUserRepository.saveAll(companyUsersToSave);
        locationUserRepository.saveAll(locationUsersToSave);

        log.info("Created Company and Location associations for {} users.", savedUsers.size());
    }

    private List<InventoryItem> createInventoryItems(Company company, List<Category> categories, List<UnitOfMeasure> uoms, List<Supplier> suppliers) {
        log.info("Creating {} Inventory Items...", NUM_INVENTORY_ITEMS);
        List<InventoryItem> items = new ArrayList<>();
        List<PurchaseOption> allPurchaseOptions = new ArrayList<>();

        // Group UOMs by their category ID for later use
        Map<Long, List<UnitOfMeasure>> uomsByCategory = uoms.stream()
                .filter(uom -> uom.getCategory() != null)
                .collect(Collectors.groupingBy(uom -> uom.getCategory().getId()));

        // Ensure we have a fallback UOM list if categorization fails
        List<UnitOfMeasure> countUoms = uoms.stream()
                .filter(u -> u.getCategory() != null && UoMCategory.COUNT.name().equals(u.getCategory().getName()))
                .toList();
        if (countUoms.isEmpty()) {
            countUoms = uoms.stream().filter(u -> "ea".equalsIgnoreCase(u.getAbbreviation())).toList();
        }
        if (countUoms.isEmpty()) {
            countUoms = uoms; // Fallback to all UOMs if 'ea' not found
        }

        for (int i = 1; i <= NUM_INVENTORY_ITEMS; i++) {
            InventoryItem item = new InventoryItem();
            item.setName("Item " + i);
            item.setSku("SKU" + i);
            item.setProductCode("PROD" + i);
            item.setDescription("Description for Item " + i);
            item.setCurrentPrice(random.nextDouble(1.0, 100.0));
            item.setCalories(random.nextDouble(50.0, 1000.0));
            item.setCompany(company);
            Category randomCategory = getRandomElement(categories, random);
            item.setCategory(randomCategory);

            // Determine Inventory UOM based on Category's UOM Category
            UnitOfMeasure baseUom = null;
            if (randomCategory != null) {
                // Find the UOM category associated with the item category (e.g., Food -> MASS)
                // This requires linking Item Category to UOM Category, which isn't directly modeled.
                // We'll infer based on typical associations or default to COUNT.
                // For simplicity, let's assume Food/Bev are MASS/VOLUME, others COUNT.
                // A better approach would be a direct link or configuration.

                List<UnitOfMeasure> potentialBaseUoms = null;
                if ("Food".equals(randomCategory.getName())) {
                    potentialBaseUoms = uomsByCategory.values().stream()
                            .flatMap(List::stream)
                            .filter(u -> u.getCategory() != null && UoMCategory.MASS.name().equals(u.getCategory().getName()))
                            .toList();
                } else if ("Beverages".equals(randomCategory.getName())) {
                    potentialBaseUoms = uomsByCategory.values().stream()
                            .flatMap(List::stream)
                            .filter(u -> u.getCategory() != null && UoMCategory.VOLUME.name().equals(u.getCategory().getName()))
                            .toList();
                }

                if (potentialBaseUoms == null || potentialBaseUoms.isEmpty()) {
                    potentialBaseUoms = countUoms; // Default to COUNT UOMs
                }

                // Prefer the base unit (conversion factor 1.0) within the category
                baseUom = potentialBaseUoms.stream()
                        .filter(u -> u.getConversionFactor() != null && u.getConversionFactor() == 1.0)
                        .findFirst()
                        .orElse(getRandomElement(potentialBaseUoms, random)); // Fallback to random within category
            }

            // Final fallback if no category or UOM found
            if (baseUom == null) {
                baseUom = getRandomElement(countUoms, random);
            }
            item.setInventoryUom(baseUom);

            InventoryItem savedItem = inventoryItemRepository.save(item);
            items.add(savedItem);

            // Create Purchase Options
            int numPOs = random.nextInt(1, 4);
            List<PurchaseOption> itemPOs = new ArrayList<>();
            // Get UOMs compatible with the item's base UOM category
            List<UnitOfMeasure> compatibleOrderingUoms = baseUom != null && baseUom.getCategory() != null
                    ? uomsByCategory.getOrDefault(baseUom.getCategory().getId(), uoms)
                    : uoms;
            if (compatibleOrderingUoms.isEmpty()) compatibleOrderingUoms = uoms; // Ensure list is not empty

            for (int j = 1; j <= numPOs; j++) {
                PurchaseOption po = new PurchaseOption();
                po.setNickname(savedItem.getName() + " PO " + j);
                po.setInventoryItem(savedItem);
                po.setSupplier(getRandomElement(suppliers, random));
                // Select an ordering UOM from the compatible list
                po.setOrderingUom(getRandomElement(compatibleOrderingUoms, random));
                po.setPrice(random.nextDouble(0.5, 90.0));
                po.setTaxRate(15.0);
                po.setSupplierProductCode("SUPP_PROD_" + i + "_" + j);
                po.setScanBarcode("BARCODE_" + i + "_" + j);
                po.setInnerPackQuantity((double) random.nextInt(1, 11));
                po.setPacksPerCase((double) random.nextInt(1, 6));
                po.setMinOrderQuantity((double) random.nextInt(1, 21));
                po.setOrderingEnabled(true);
                po.setMainPurchaseOption(j == 1);
                itemPOs.add(po);
            }
            allPurchaseOptions.addAll(itemPOs);
        }

        purchaseOptionRepository.saveAll(allPurchaseOptions);
        log.info("Created {} Inventory Items and {} Purchase Options.", items.size(), allPurchaseOptions.size());
        return items;
    }

    private List<SubRecipe> createSubRecipes(Company company, List<Category> categories, List<UnitOfMeasure> uoms, List<InventoryItem> inventoryItems, Map<Long, List<UnitOfMeasure>> uomsByCategory) {
        log.info("Creating {} Sub Recipes...", NUM_SUB_RECIPES);
        List<SubRecipe> savedSubRecipes = new ArrayList<>();
        if (inventoryItems.isEmpty()) {
            log.warn("No inventory items available to create sub recipe lines. Skipping sub recipe creation.");
            return savedSubRecipes;
        }

        for (int i = 1; i <= NUM_SUB_RECIPES; i++) {
            SubRecipeType type = random.nextBoolean() ? SubRecipeType.PREPARATION : SubRecipeType.SUB_RECIPE;
            String name = (type == SubRecipeType.PREPARATION ? "Prep " : "Recipe ") + i;
            Category category = getRandomElement(categories, random);
            UnitOfMeasure yieldUom = determineAppropriateUom(category, uomsByCategory, uoms, random);

            SubRecipeCreateDTO dto = new SubRecipeCreateDTO();
            dto.setName(name);
            dto.setType(type);
            dto.setCategoryId(category != null ? category.getId() : null);
            dto.setUomId(yieldUom != null ? yieldUom.getId() : null);
            dto.setYieldQty(random.nextDouble(1.0, 20.0));
            dto.setPhotoUrl("http://example.com/photo" + i + ".jpg");
            dto.setPrepTimeMinutes(random.nextInt(5, 61));
            dto.setCookTimeMinutes(random.nextInt(0, 121));
            dto.setInstructions("Instructions for " + name);

            int numLines = random.nextInt(2, 6);
            List<SubRecipeLineDTO> lines = new ArrayList<>();
            for (int j = 0; j < numLines; j++) {
                InventoryItem ingredient = getRandomElement(inventoryItems, random);
                if (ingredient == null || ingredient.getInventoryUom() == null || ingredient.getInventoryUom().getCategory() == null) continue;

                Long ingredientUomCategoryId = ingredient.getInventoryUom().getCategory().getId();
                List<UnitOfMeasure> compatibleLineUoms = uomsByCategory.getOrDefault(ingredientUomCategoryId, uoms);
                if (compatibleLineUoms.isEmpty()) compatibleLineUoms = uoms;

                UnitOfMeasure lineUom = getRandomElement(compatibleLineUoms, random);
                if (lineUom == null) continue;

                SubRecipeLineDTO lineDto = new SubRecipeLineDTO();
                lineDto.setInventoryItemId(ingredient.getId());
                lineDto.setQuantity(random.nextDouble(0.1, 5.0));
                lineDto.setWastagePercent(random.nextDouble(0.0, 10.0));
                lineDto.setUnitOfMeasureId(lineUom.getId());
                lines.add(lineDto);
            }
            dto.setLines(lines);

            try {
                if (dto.getUomId() == null) {
                    log.warn("SubRecipe '{}' is missing a yield UOM. Assigning default.", name);
                    UnitOfMeasure defaultUom = uoms.stream().filter(u -> "ea".equalsIgnoreCase(u.getAbbreviation())).findFirst().orElse(getRandomElement(uoms, random));
                    if (defaultUom != null) dto.setUomId(defaultUom.getId());
                }

                if (dto.getUomId() != null && !lines.isEmpty()) {
                    SubRecipe saved = subRecipeService.createSubRecipe(company.getId(), dto);
                    savedSubRecipes.add(saved);
                } else {
                    log.warn("Skipping creation of SubRecipe '{}' due to missing yield UOM or lines.", name);
                }
            } catch (Exception e) {
                log.error("Failed to create SubRecipe '{}': {}", name, e.getMessage(), e);
            }
        }
        log.info("Created {} Sub Recipes.", savedSubRecipes.size());
        return savedSubRecipes;
    }

    private void createMenuItems(Company company, List<Category> categories, List<UnitOfMeasure> uoms, List<InventoryItem> inventoryItems, List<SubRecipe> subRecipes, Map<Long, List<UnitOfMeasure>> uomsByCategory) {
        log.info("Creating {} Menu Items...", NUM_MENU_ITEMS);
        List<MenuItem> savedMenuItems = new ArrayList<>();
        if (inventoryItems.isEmpty() && subRecipes.isEmpty()) {
            log.warn("No inventory items or sub-recipes available to create menu item lines. Skipping menu item creation.");
            return;
        }

        for (int i = 1; i <= NUM_MENU_ITEMS; i++) {
            String name = "Menu Item " + i;
            String posCode = "POS" + (1000 + i);
            Category category = getRandomElement(categories, random);

            MenuItemCreateDTO dto = new MenuItemCreateDTO();
            dto.setName(name);
            dto.setPosCode(posCode);
            dto.setCategoryId(category != null ? category.getId() : null);
            dto.setRetailPriceExclTax(random.nextDouble(5.0, 150.0));
            dto.setMaxAllowedFoodCostPct(random.nextDouble(25.0, 40.0));

            int numLines = random.nextInt(1, 6);
            List<MenuItemLineDTO> lines = new ArrayList<>();
            for (int j = 0; j < numLines; j++) {
                MenuItemLineDTO lineDto = new MenuItemLineDTO();
                boolean useItem = inventoryItems.isEmpty() ? false : (subRecipes.isEmpty() ? true : random.nextBoolean());

                UnitOfMeasure lineUom = null;
                List<UnitOfMeasure> compatibleLineUoms = uoms;

                if (useItem) {
                    InventoryItem item = getRandomElement(inventoryItems, random);
                    if (item == null || item.getInventoryUom() == null || item.getInventoryUom().getCategory() == null) continue;
                    lineDto.setInventoryItemId(item.getId());
                    Long itemUomCategoryId = item.getInventoryUom().getCategory().getId();
                    compatibleLineUoms = uomsByCategory.getOrDefault(itemUomCategoryId, uoms);
                } else {
                    SubRecipe sub = getRandomElement(subRecipes, random);
                    if (sub == null || sub.getUom() == null || sub.getUom().getCategory() == null) continue;
                    lineDto.setSubRecipeId(sub.getId());
                    Long subUomCategoryId = sub.getUom().getCategory().getId();
                    compatibleLineUoms = uomsByCategory.getOrDefault(subUomCategoryId, uoms);
                }

                if (compatibleLineUoms.isEmpty()) compatibleLineUoms = uoms;
                lineUom = getRandomElement(compatibleLineUoms, random);

                if (lineUom == null) continue;

                lineDto.setQuantity(random.nextDouble(0.05, 3.0));
                lineDto.setWastagePercent(random.nextDouble(0.0, 15.0));
                lineDto.setUnitOfMeasureId(lineUom.getId());
                lines.add(lineDto);
            }

            if (lines.isEmpty() && (!inventoryItems.isEmpty() || !subRecipes.isEmpty())) {
                MenuItemLineDTO fallbackLine = createFallbackMenuItemLine(inventoryItems, subRecipes, uoms, uomsByCategory, random);
                if (fallbackLine != null) {
                    lines.add(fallbackLine);
                }
            }

            dto.setMenuItemLines(lines);

            if (!lines.isEmpty()) {
                try {
                    MenuItem saved = menuItemService.createMenuItem(company.getId(), dto);
                    savedMenuItems.add(saved);
                } catch (Exception e) {
                    log.error("Failed to create MenuItem '{}' with DTO: {}. Error: {}", name, dto, e.getMessage(), e);
                }
            } else {
                log.warn("Skipping creation of MenuItem '{}' because no valid lines could be generated.", name);
            }
        }
        log.info("Attempted to create {} Menu Items, successfully created {}.", NUM_MENU_ITEMS, savedMenuItems.size());
    }

    private UnitOfMeasure determineAppropriateUom(Category category, Map<Long, List<UnitOfMeasure>> uomsByCategory, List<UnitOfMeasure> allUoms, Random random) {
        List<UnitOfMeasure> potentialUoms = null;
        List<UnitOfMeasure> countUoms = uomsByCategory.values().stream().flatMap(List::stream).filter(u -> u.getCategory() != null && UoMCategory.COUNT.name().equals(u.getCategory().getName())).toList();
        if (countUoms.isEmpty()) countUoms = allUoms.stream().filter(u -> "ea".equalsIgnoreCase(u.getAbbreviation())).toList();
        if (countUoms.isEmpty()) countUoms = allUoms;

        if (category != null) {
            if ("Food".equals(category.getName())) {
                potentialUoms = uomsByCategory.values().stream().flatMap(List::stream).filter(u -> u.getCategory() != null && UoMCategory.MASS.name().equals(u.getCategory().getName())).toList();
            } else if ("Beverages".equals(category.getName())) {
                potentialUoms = uomsByCategory.values().stream().flatMap(List::stream).filter(u -> u.getCategory() != null && UoMCategory.VOLUME.name().equals(u.getCategory().getName())).toList();
            }
        }

        if (potentialUoms == null || potentialUoms.isEmpty()) {
            potentialUoms = countUoms;
        }

        return getRandomElement(potentialUoms, random);
    }

    private MenuItemLineDTO createFallbackMenuItemLine(List<InventoryItem> inventoryItems, List<SubRecipe> subRecipes, List<UnitOfMeasure> uoms, Map<Long, List<UnitOfMeasure>> uomsByCategory, Random random) {
        for (int attempt = 0; attempt < 5; attempt++) {
            MenuItemLineDTO lineDto = new MenuItemLineDTO();
            boolean useItem = inventoryItems.isEmpty() ? false : (subRecipes.isEmpty() ? true : random.nextBoolean());
            UnitOfMeasure lineUom = null;
            List<UnitOfMeasure> compatibleLineUoms = uoms;

            if (useItem) {
                InventoryItem item = getRandomElement(inventoryItems, random);
                if (item != null && item.getInventoryUom() != null && item.getInventoryUom().getCategory() != null) {
                    lineDto.setInventoryItemId(item.getId());
                    Long itemUomCategoryId = item.getInventoryUom().getCategory().getId();
                    compatibleLineUoms = uomsByCategory.getOrDefault(itemUomCategoryId, uoms);
                } else continue;
            } else {
                SubRecipe sub = getRandomElement(subRecipes, random);
                if (sub != null && sub.getUom() != null && sub.getUom().getCategory() != null) {
                    lineDto.setSubRecipeId(sub.getId());
                    Long subUomCategoryId = sub.getUom().getCategory().getId();
                    compatibleLineUoms = uomsByCategory.getOrDefault(subUomCategoryId, uoms);
                } else continue;
            }

            if (compatibleLineUoms.isEmpty()) compatibleLineUoms = uoms;
            lineUom = getRandomElement(compatibleLineUoms, random);

            if (lineUom != null) {
                lineDto.setQuantity(random.nextDouble(0.05, 3.0));
                lineDto.setWastagePercent(random.nextDouble(0.0, 15.0));
                lineDto.setUnitOfMeasureId(lineUom.getId());
                return lineDto;
            }
        }
        return null;
    }

    private <T> T getRandomElement(List<T> list, Random random) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(random.nextInt(list.size()));
    }
}
