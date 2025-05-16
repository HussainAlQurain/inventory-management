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
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.lang.InterruptedException;
import java.util.stream.Collectors;

@Configuration
@Profile("!test") // Don't run during tests
public class DataInitializerConfig implements CommandLineRunner, ApplicationContextAware {

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
    private static final String SYSTEM_USERNAME = "system-user";

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
    private final PlatformTransactionManager transactionManager;
    private final TransactionTemplate transactionTemplate;
    private final AutoOrderSettingRepository autoOrderSettingRepository;
    private final AutoRedistributeSettingRepository autoRedistributeSettingRepository;
    private final LocationIntegrationSettingRepository locationIntegrationSettingRepository;

    private ApplicationContext applicationContext;

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
                                 PasswordEncoder passwordEncoder,
                                 PlatformTransactionManager transactionManager,
                                 AutoOrderSettingRepository autoOrderSettingRepository,
                                 AutoRedistributeSettingRepository autoRedistributeSettingRepository,
                                 LocationIntegrationSettingRepository locationIntegrationSettingRepository) {
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
        this.transactionManager = transactionManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.autoOrderSettingRepository = autoOrderSettingRepository;
        this.autoRedistributeSettingRepository = autoRedistributeSettingRepository;
        this.locationIntegrationSettingRepository = locationIntegrationSettingRepository;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    @Transactional // Use transaction for atomicity
    public void run(String... args) throws Exception {
        log.info("Checking if data initialization is needed...");

        // First, initialize core system data that was previously in data.sql
        initializeCoreSystemData();

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

    public void initializeCoreSystemData() {
        log.info("Initializing core system data...");

        // 1. Create all roles (method remains @Transactional)
        initializeRoles();

        // 2. Create admin user (method remains @Transactional)
        Users admin = initializeAdminUser();

        // 3. Create system user (method remains @Transactional)
        Users systemUser = initializeSystemUser();

        // 4. Create company (method remains @Transactional)
        Company company = initializeCompany();

        // 5. Create default location (method remains @Transactional)
        Location defaultLocation = initializeDefaultLocation(company);

        // 6. Link admin user to roles (method remains @Transactional)
        initializeAdminUserRoles(admin);

        // 7. Link system user to roles (method remains @Transactional)
        initializeSystemUserRoles(systemUser);

        // 8. Link admin user to company (method remains @Transactional)
        initializeCompanyUser(admin, company);

        // 9. Link admin user to default location (method remains @Transactional)
        initializeLocationUser(admin, defaultLocation);

        // 10. Initialize auto-order settings for 50 locations
        initializeAutoOrderSettings(company.getId(), systemUser.getId());

        // 11. Initialize auto-redistribute setting for the company
        initializeAutoRedistributeSetting(company.getId());

        // 12. Initialize integration settings for 50 locations
        initializeLocationIntegrationSettings(company.getId());

        log.info("Core system data initialization completed");
    }

    @Transactional
    public void initializeRoles() {
        log.info("Initializing roles...");

        List<userRoles> requiredRoles = List.of(
                userRoles.ROLE_USER,
                userRoles.ROLE_STAFF,
                userRoles.ROLE_MANAGER,
                userRoles.ROLE_ADMIN,
                userRoles.ROLE_SUPER_ADMIN,
                userRoles.ROLE_SYSTEM_ADMIN
        );

        for (userRoles roleName : requiredRoles) {
            if (roleRepository.findByName(roleName) == null) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
                log.info("Created role: {}", roleName);
            } else {
                log.info("Role already exists: {}", roleName);
            }
        }
    }

    @Transactional
    public Users initializeAdminUser() {
        log.info("Initializing admin user...");

        Optional<Users> adminOpt = userRepository.findByUsername("admin");
        if (adminOpt.isEmpty()) {
            Users admin = new Users();
            admin.setUsername("admin");
            admin.setPassword("$2a$14$C2HvKTOQmGVMKZGQ0xa1NO8UUcRHoYgjESdZlEj51bZcSKye43Qdm"); // encoded "admin"
            admin.setEmail("hussain.qurain@outlook.com");
            admin.setStatus("active");
            admin.setFirstName("Hussain");
            admin.setLastName("Al-Qurain");
            admin.setPhone("+966536071929");
            admin.setRoles(new HashSet<>());
            admin = userRepository.save(admin);
            log.info("Created admin user with ID: {}", admin.getId());
            return admin;
        } else {
            log.info("Admin user already exists with ID: {}", adminOpt.get().getId());
            return adminOpt.get();
        }
    }

    @Transactional
    public Users initializeSystemUser() {
        log.info("Initializing system user...");

        // First check if user exists by username in a new transaction
        Optional<Users> existingUser = transactionTemplate.execute(status -> userRepository.findByUsername(SYSTEM_USERNAME));

        if (existingUser.isPresent()) {
            log.info("System user already exists with username: {}", SYSTEM_USERNAME);
            return existingUser.get();
        }

        // If not present, attempt to create
        Users systemUser = new Users();
        systemUser.setUsername(SYSTEM_USERNAME);
        systemUser.setPassword("$2a$14$C2HvKTOQmGVMKZGQ0xa1NO8UUcRHoYgjESdZlEj51bZcSKye43Qdm"); // same encoded password
        systemUser.setEmail("system@example.com");
        systemUser.setStatus("active");
        systemUser.setFirstName("SYSTEM");
        systemUser.setLastName("USER");
        systemUser.setPhone("+000000000000");
        systemUser.setRoles(new HashSet<>());

        try {
            // Try saving the new user, let sequence generate ID
            Users savedUser = userRepository.save(systemUser);
            log.info("Created system user with username: {} and ID: {}", savedUser.getUsername(), savedUser.getId());
            return savedUser;
        } catch (DataIntegrityViolationException e) {
            // Catch potential unique constraint violation (username)
            log.warn("DataIntegrityViolationException during system user creation (username: {}), likely race condition. Fetching existing.",
                    SYSTEM_USERNAME, e);

            // If save fails due to conflict, fetch it by username in a NEW transaction
            // Add retries just in case of visibility delays
            for (int i = 0; i < 3; i++) {
                try {
                    if (i > 0) {
                        TimeUnit.MILLISECONDS.sleep(100 * i); // Wait 100ms, 200ms
                        log.info("Retrying fetch for system user (username: {}) attempt {}", SYSTEM_USERNAME, i + 1);
                    }
                    Optional<Users> userAfterConflict = transactionTemplate.execute(status ->
                            userRepository.findByUsername(SYSTEM_USERNAME)
                    );
                    if (userAfterConflict.isPresent()) {
                        log.info("Successfully fetched system user (username: {}) after conflict on attempt {}", SYSTEM_USERNAME, i + 1);
                        return userAfterConflict.get();
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting to retry fetch for system user (username: " + SYSTEM_USERNAME + ")", ie);
                } catch (Exception fetchEx) {
                    log.error("Error during retry fetch for system user (username: {}) on attempt {}: {}", SYSTEM_USERNAME, i + 1, fetchEx.getMessage(), fetchEx);
                }
            }
            // If all retries fail, throw the exception
            throw new RuntimeException("Failed to fetch system user (username: " + SYSTEM_USERNAME + ") after data integrity violation and retries.", e);
        } catch (ObjectOptimisticLockingFailureException oolfe) {
            // Keep handling optimistic lock just in case, though less likely now
            log.warn("ObjectOptimisticLockingFailureException during system user creation (username: {}), unexpected but handling. Fetching existing.",
                    SYSTEM_USERNAME, oolfe);
            // Fetch by username in a new transaction
            return transactionTemplate.execute(status ->
                    userRepository.findByUsername(SYSTEM_USERNAME)
                            .orElseThrow(() -> new RuntimeException("Failed to fetch system user (username: " + SYSTEM_USERNAME + ") after optimistic lock failure.", oolfe))
            );
        }
    }

    @Transactional
    public Company initializeCompany() {
        log.info("Initializing company...");

        Company company = null;
        List<Company> companies = companyRepository.findByName("Company A");

        if (companies.isEmpty()) {
            company = new Company();
            company.setName("Company A");
            company.setTaxId("3000000000000");
            company.setPhone("+966013555555");
            company.setMobile("+966555555555");
            company.setEmail("hussain.qurain@outlook.com");
            company.setState("khobar");
            company.setCity("khobar");
            company.setAddress("khobar");
            company.setZip("55555");
            company.setAddPurchasedItemsToFavorites(true);
            company.setLogo("test.png");
            company.setAllowedInvoiceDeviation(3.0);
            company.setExportDeliveryNotesAsBills(true);
            company = companyRepository.save(company);
            log.info("Created company with ID: {}", company.getId());
        } else {
            company = companies.getFirst();
            log.info("Company already exists with ID: {}", company.getId());
        }

        return company;
    }

    @Transactional
    public Location initializeDefaultLocation(Company company) {
        log.info("Initializing default location for company ID: {}", company.getId());

        Location location = null;
        List<Location> locations = locationRepository.findByNameAndCompanyId("Default Location", company.getId());

        if (locations.isEmpty()) {
            location = new Location();
            location.setName("Default Location");
            location.setCode("LOC1");
            location.setAddress("123 Main St");
            location.setCity("Khobar");
            location.setState("Eastern Province");
            location.setZip("12345");
            location.setPhone("+966123456789");
            location.setCompany(company);
            location = locationRepository.save(location);
            log.info("Created default location with ID: {}", location.getId());
        } else {
            location = locations.get(0);
            log.info("Default location already exists with ID: {}", location.getId());
        }

        return location;
    }

    @Transactional
    public void initializeAdminUserRoles(Users admin) {
        log.info("Initializing roles for admin user...");

        Role userRole = roleRepository.findByName(userRoles.ROLE_USER);
        Role adminRole = roleRepository.findByName(userRoles.ROLE_ADMIN);
        Role superAdminRole = roleRepository.findByName(userRoles.ROLE_SUPER_ADMIN);

        Set<Role> currentRoles = admin.getRoles();
        if (currentRoles == null) {
            currentRoles = new HashSet<>();
            admin.setRoles(currentRoles);
        }

        if (!currentRoles.contains(userRole)) {
            currentRoles.add(userRole);
            log.info("Added USER role to admin user");
        }

        if (!currentRoles.contains(adminRole)) {
            currentRoles.add(adminRole);
            log.info("Added ADMIN role to admin user");
        }

        if (!currentRoles.contains(superAdminRole)) {
            currentRoles.add(superAdminRole);
            log.info("Added SUPER_ADMIN role to admin user");
        }

        userRepository.save(admin);
    }

    @Transactional
    public void initializeSystemUserRoles(Users systemUser) {
        log.info("Initializing roles for system user...");

        Role superAdminRole = roleRepository.findByName(userRoles.ROLE_SUPER_ADMIN);

        Set<Role> currentRoles = systemUser.getRoles();
        if (currentRoles == null) {
            currentRoles = new HashSet<>();
            systemUser.setRoles(currentRoles);
        }

        boolean updated = false;
        if (superAdminRole != null && !currentRoles.contains(superAdminRole)) {
            currentRoles.add(superAdminRole);
            log.info("Added SUPER_ADMIN role to system user");
            updated = true; // Mark as updated if a role was added
        } else if (superAdminRole == null) {
            log.warn("SUPER_ADMIN role not found, cannot assign to system user.");
        }

        // Remove the explicit save call. Changes to the managed 'systemUser'
        // will be flushed automatically at transaction commit if 'updated' is true.
        // userRepository.save(systemUser); 
        if (!updated) {
            log.info("System user roles already up-to-date.");
        }
    }

    @Transactional
    public void initializeCompanyUser(Users admin, Company company) {
        log.info("Linking admin user to company...");

        // Check if the association already exists - using the correct method findByCompanyIdAndUserId
        boolean exists = companyUserRepository.findByCompanyIdAndUserId(company.getId(), admin.getId()).isPresent();

        if (!exists) {
            CompanyUser companyUser = new CompanyUser();
            companyUser.setCompany(company);
            companyUser.setUser(admin);
            companyUserRepository.save(companyUser);
            log.info("Linked admin user to company");
        } else {
            log.info("Admin user already linked to company");
        }
    }

    @Transactional
    public void initializeLocationUser(Users admin, Location location) {
        log.info("Linking admin user to default location...");

        // Check if the association already exists
        boolean exists = locationUserRepository.findByLocationIdAndUserId(location.getId(), admin.getId()).isPresent();

        if (!exists) {
            LocationUser locationUser = new LocationUser();
            locationUser.setLocation(location);
            locationUser.setUser(admin);
            locationUserRepository.save(locationUser);
            log.info("Linked admin user to default location");
        } else {
            log.info("Admin user already linked to default location");
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

        log.info("Preparing {} inventory items for batch insert...", NUM_INVENTORY_ITEMS);

        // Create all items at once for batch insert
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

            items.add(item);
        }

        // Batch save all items
        log.info("Batch saving {} inventory items...", items.size());
        List<InventoryItem> savedItems = inventoryItemRepository.saveAll(items);
        log.info("Successfully saved {} inventory items.", savedItems.size());

        // Create Purchase Options in batches
        log.info("Creating purchase options...");
        int batchSize = 100;
        for (int i = 0; i < savedItems.size(); i++) {
            InventoryItem savedItem = savedItems.get(i);

            // Get UOMs compatible with the item's base UOM category
            List<UnitOfMeasure> compatibleOrderingUoms = savedItem.getInventoryUom() != null && savedItem.getInventoryUom().getCategory() != null
                    ? uomsByCategory.getOrDefault(savedItem.getInventoryUom().getCategory().getId(), uoms)
                    : uoms;
            if (compatibleOrderingUoms.isEmpty()) compatibleOrderingUoms = uoms; // Ensure list is not empty

            // Create Purchase Options
            int numPOs = random.nextInt(1, 4);
            for (int j = 1; j <= numPOs; j++) {
                PurchaseOption po = new PurchaseOption();
                po.setNickname(savedItem.getName() + " PO " + j);
                po.setInventoryItem(savedItem);
                po.setSupplier(getRandomElement(suppliers, random));
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
                allPurchaseOptions.add(po);
            }

            // Save purchase options in batches
            if (i % batchSize == batchSize - 1 || i == savedItems.size() - 1) {
                log.info("Batch saving {} purchase options (items {}-{})...", allPurchaseOptions.size(), Math.max(0, i - batchSize + 1), i);
                purchaseOptionRepository.saveAll(allPurchaseOptions);
                log.info("Successfully saved batch of purchase options.");
                allPurchaseOptions.clear();  // Clear after saving to free memory
            }
        }

        log.info("Completed creating {} Inventory Items with purchase options.", savedItems.size());
        return savedItems;
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

    @Transactional
    public void initializeAutoOrderSettings(Long companyId, Long systemUserId) {
        log.info("Initializing auto-order settings for 50 locations...");

        // Get first 50 locations for the company
        List<Location> locations = locationRepository.findByCompanyId(companyId)
                .stream()
                .limit(50)
                .collect(Collectors.toList());

        if (locations.isEmpty()) {
            log.warn("No locations found for company ID: {}. Skipping auto-order settings initialization.", companyId);
            return;
        }

        int count = 0;
        for (Location location : locations) {
            // Check if setting already exists
            if (autoOrderSettingRepository.findByLocationId(location.getId()).isPresent()) {
                log.debug("Auto-order setting already exists for location ID: {}. Updating it.", location.getId());
            }

            AutoOrderSetting setting = autoOrderSettingRepository.findByLocationId(location.getId())
                    .orElse(new AutoOrderSetting(null, location, false, 300, null, null, null));

            // Update the setting
            setting.setEnabled(true);
            setting.setFrequencySeconds(300); // 5 minutes
            setting.setSystemUserId(systemUserId);
            setting.setAutoOrderComment("Auto-generated order for inventory replenishment");

            autoOrderSettingRepository.save(setting);
            count++;
        }

        log.info("Initialized auto-order settings for {} locations", count);
    }

    @Transactional
    public void initializeAutoRedistributeSetting(Long companyId) {
        log.info("Initializing auto-redistribute setting for company ID: {}...", companyId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found with ID: " + companyId));

        // Check if setting already exists
        AutoRedistributeSetting setting = autoRedistributeSettingRepository.findByCompanyId(companyId)
                .orElse(new AutoRedistributeSetting());

        // Set company if it's a new setting
        if (setting.getCompany() == null) {
            setting.setCompany(company);
        }

        // Update the setting
        setting.setEnabled(true);
        setting.setFrequencySeconds(30); // 5 minutes
        setting.setAutoTransferComment("Auto-generated transfer for stock balancing");

        autoRedistributeSettingRepository.save(setting);

        log.info("Initialized auto-redistribute setting for company ID: {}", companyId);
    }

    @Transactional
    public void initializeLocationIntegrationSettings(Long companyId) {
        log.info("Initializing integration settings for 50 locations...");

        // Get first 50 locations for the company
        List<Location> locations = locationRepository.findByCompanyId(companyId)
                .stream()
                .limit(50)
                .collect(Collectors.toList());

        if (locations.isEmpty()) {
            log.warn("No locations found for company ID: {}. Skipping integration settings initialization.", companyId);
            return;
        }

        log.info("Found {} locations to initialize integration settings for", locations.size());

        int count = 0;
        for (Location location : locations) {
            try {
                // Base URL for the POS API - update to match the new format
                String posApiUrl = "http://localhost:8888/api/sales/location/" + location.getId();

                // Check if setting already exists
                Optional<LocationIntegrationSetting> existingSetting = locationIntegrationSettingRepository.findByLocationId(location.getId());
                
                LocationIntegrationSetting setting;
                if (existingSetting.isPresent()) {
                    log.info("Updating existing integration setting for location: {}", location.getName());
                    setting = existingSetting.get();
                } else {
                    log.info("Creating new integration setting for location: {}", location.getName());
                    setting = new LocationIntegrationSetting();
                    setting.setLocation(location);
                }

                // Update the setting with 30 second frequency for easier testing
                setting.setPosApiUrl(posApiUrl);
                setting.setFrequentSyncSeconds(30); // 30 seconds for faster testing
                setting.setFrequentSyncEnabled(true);
                setting.setDailySyncEnabled(true);

                LocationIntegrationSetting saved = locationIntegrationSettingRepository.save(setting);
                if (saved != null && saved.getId() != null) {
                    count++;
                    log.info("Successfully saved integration setting for location: {}", location.getName());
                } else {
                    log.error("Failed to save integration setting for location: {}", location.getName());
                }
            } catch (Exception e) {
                log.error("Error initializing integration setting for location {}: {}", location.getName(), e.getMessage(), e);
            }
        }

        log.info("Initialized integration settings for {} out of {} locations", count, locations.size());
    }
}
