package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.LocationIntegrationSetting;
import com.rayvision.inventory_management.model.Sale;
import com.rayvision.inventory_management.model.dto.*;
import com.rayvision.inventory_management.repository.LocationIntegrationSettingRepository;
import com.rayvision.inventory_management.repository.SaleRepository;
import com.rayvision.inventory_management.service.SaleService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PosIntegrationService {
    private final RestTemplate restTemplate = new RestTemplate();

    private final LocationIntegrationSettingRepository settingRepo;
    private final SaleService saleService; // your existing service
    private final SaleRepository saleRepository; // if you need direct queries

    public PosIntegrationService(
            LocationIntegrationSettingRepository settingRepo,
            SaleService saleService,
            SaleRepository saleRepository
    ) {
        this.settingRepo = settingRepo;
        this.saleService = saleService;
        this.saleRepository = saleRepository;
    }

    /**
     * 1) Frequent job:
     *    runs every 120s.  We check if it’s time for each location
     *    (based on lastFrequentSyncTime + frequentSyncSeconds).
     */
    @Scheduled(fixedDelay = 120000) // 120 seconds
    public void frequentSyncTask() {
        List<LocationIntegrationSetting> settings = settingRepo.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (LocationIntegrationSetting s : settings) {
            if (!s.isFrequentSyncEnabled()) {
                continue; // skip if not enabled
            }
            if (s.getFrequentSyncSeconds() == null || s.getFrequentSyncSeconds() <= 0) {
                continue; // skip invalid config
            }

            LocalDateTime last = Optional.ofNullable(s.getLastFrequentSyncTime())
                    .orElse(LocalDateTime.of(1970,1,1,0,0));
            LocalDateTime nextAllowed = last.plusSeconds(s.getFrequentSyncSeconds());

            if (now.isAfter(nextAllowed)) {
                // time to sync
                try {
                    doFrequentSyncForLocation(s);
                    // if success => update s.setLastFrequentSyncTime(now)
                    s.setLastFrequentSyncTime(now);
                    settingRepo.save(s);
                } catch (Exception ex) {
                    // log error, skip
                }
            }
        }
    }

    /**
     * 2) Daily job: run at midnight
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void dailySyncTask() {
        List<LocationIntegrationSetting> settings = settingRepo.findAll();
        for (LocationIntegrationSetting s : settings) {
            if (!s.isDailySyncEnabled()) {
                continue;
            }
            try {
                doDailySyncForLocation(s);
            } catch (Exception ex) {
                // log error
            }
        }
    }

    private void doFrequentSyncForLocation(LocationIntegrationSetting s) {
        // We decide to fetch only "today's" sales from POS
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        int page = (s.getLastFrequentPageSynced() != null) ? s.getLastFrequentPageSynced() : 0;
        boolean morePages = true;

        while(morePages) {
            // Format dates properly for URL
            String startDateStr = startOfDay.toString(); // This formats as ISO 8601
            String endDateStr = end.toString();
            
            String url = String.format("%s?startDate=%s&endDate=%s&page=%d&size=50",
                    s.getPosApiUrl(),
                    startDateStr,
                    endDateStr,
                    page
            );
            
            System.out.println("Fetching sales from URL: " + url);
            
            try {
                // Get raw response first for debugging
                String rawResponse = restTemplate.getForObject(url, String.class);
                System.out.println("Raw API response: " + rawResponse);
                
                // Now try to parse it
                PosSalesPagedResponse response = restTemplate.getForObject(url, PosSalesPagedResponse.class);
                if (response == null) {
                    System.out.println("Response is null for location " + s.getLocation().getId());
                    morePages = false;
                } else {
                    System.out.println("Response: totalPages=" + response.getTotalPages() + ", sales=" + (response.getContent() == null ? "null" : response.getContent().size()));
                    
                    if (response.getContent() == null || response.getContent().isEmpty()) {
                        System.out.println("No sales found in response for location " + s.getLocation().getId());
                        morePages = false;
                    } else {
                        System.out.println("Found " + response.getContent().size() + " sales for location " + s.getLocation().getId());
                        for (PosSaleDTO saleDto : response.getContent()) {
                            importSale(saleDto);
                        }
                        if (page >= response.getTotalPages() - 1) {
                            morePages = false;
                        } else {
                            page++;
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("Error in doFrequentSyncForLocation: " + ex.getMessage());
                ex.printStackTrace();
                morePages = false;
            }
        }
        
        // if all done => reset page
        s.setLastFrequentPageSynced(0);
        settingRepo.save(s);
    }

    private void doDailySyncForLocation(LocationIntegrationSetting s) {
        // e.g. we fetch the entire month: from 1st of month to now
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDateTime start = firstOfMonth.atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        int page = (s.getLastDailyPageSynced() != null) ? s.getLastDailyPageSynced() : 0;
        boolean morePages = true;

        while(morePages) {
            // Format dates properly for URL
            String startDateStr = start.toString();
            String endDateStr = end.toString();
            
            String url = String.format("%s?startDate=%s&endDate=%s&page=%d&size=50",
                    s.getPosApiUrl(),
                    startDateStr,
                    endDateStr,
                    page
            );
            
            System.out.println("Daily sync - Fetching sales from URL: " + url);
            
            try {
                PosSalesPagedResponse response = restTemplate.getForObject(url, PosSalesPagedResponse.class);
                if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
                    morePages = false;
                } else {
                    for (PosSaleDTO saleDto : response.getContent()) {
                        importSale(saleDto);
                    }
                    if (page >= response.getTotalPages() - 1) {
                        morePages = false;
                    } else {
                        page++;
                    }
                }
            } catch (Exception ex) {
                System.err.println("Error in doDailySyncForLocation: " + ex.getMessage());
                ex.printStackTrace();
                morePages = false;
            }
        }
        
        // reset page
        s.setLastDailyPageSynced(0);
        s.setLastDailySyncTime(LocalDateTime.now());
        settingRepo.save(s);
    }

    private void importSale(PosSaleDTO saleDto) {
        // convert the POS sale => SaleCreateDTO
        SaleCreateDTO createDTO = new SaleCreateDTO();
        createDTO.setLocationId(saleDto.getLocationId());
        createDTO.setSaleDateTime(saleDto.getSaleDateTime());
        createDTO.setPosReference(saleDto.getPosReference());
        List<SaleLineCreateDTO> lineDtos = new ArrayList<>();
        for (PosLineDTO line : saleDto.getLines()) {
            SaleLineCreateDTO sc = new SaleLineCreateDTO();
            sc.setPosCode(line.getPosCode());
            sc.setMenuItemName(line.getMenuItemName());
            sc.setQuantity(line.getQuantity());
            sc.setUnitPrice(line.getUnitPrice());
            lineDtos.add(sc);
        }
        createDTO.setLines(lineDtos);

        // call your saleService
        try {
            saleService.createSale(createDTO);
        } catch (Exception ex) {
            // if duplicate or error => skip or log
        }
    }

}
