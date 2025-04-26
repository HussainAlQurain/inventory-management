package com.rayvision.inventory_management.service.impl;

import com.rayvision.inventory_management.model.AutoOrderSetting;
import com.rayvision.inventory_management.model.AutoRedistributeSetting;
import com.rayvision.inventory_management.repository.AutoOrderSettingRepository;
import com.rayvision.inventory_management.repository.AutoRedistributeSettingRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DynamicSchedulerService {

    private final RedistributeJob redistributeJob;
    private final AutoOrderScheduledService autoOrderService;
    private final AutoRedistributeSettingRepository redistributeSettingRepo;
    private final AutoOrderSettingRepository orderSettingRepo;
    private final TaskScheduler taskScheduler;
    
    // Default values from application properties
    @Value("${inventory.scheduled.redistribute.delay:60000}")
    private long defaultRedistributeDelay;
    
    @Value("${inventory.scheduled.auto-order.delay:60000}")
    private long defaultAutoOrderDelay;
    
    // Maps to track active jobs
    private final Map<Long, ScheduledFuture<?>> redistributeJobs = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> autoOrderJobs = new ConcurrentHashMap<>();
    
    // Last checked timestamps to track changes
    private final Map<Long, LocalDateTime> redistributeLastUpdated = new ConcurrentHashMap<>();
    private final Map<Long, LocalDateTime> autoOrderLastUpdated = new ConcurrentHashMap<>();

    public DynamicSchedulerService(
            RedistributeJob redistributeJob,
            AutoOrderScheduledService autoOrderService,
            AutoRedistributeSettingRepository redistributeSettingRepo,
            AutoOrderSettingRepository orderSettingRepo) {
        this.redistributeJob = redistributeJob;
        this.autoOrderService = autoOrderService;
        this.redistributeSettingRepo = redistributeSettingRepo;
        this.orderSettingRepo = orderSettingRepo;
        
        // Create dedicated scheduler with appropriate thread pool
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10); // Adjust based on your needs
        scheduler.setThreadNamePrefix("dynamic-scheduler-");
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    @PostConstruct
    public void init() {
        // Initial scheduling of all jobs
        refreshAllJobs();
    }

    // Run periodic check to update job schedules based on settings changes
    @Scheduled(fixedDelay = 60000) // Check every minute
    @Transactional(readOnly = true)
    public void refreshAllJobs() {
        log.debug("Refreshing dynamic job schedules");
        refreshRedistributeJobs();
        refreshAutoOrderJobs();
    }

    @Transactional(readOnly = true)
    public void refreshRedistributeJobs() {
        // Fetch all settings with eagerly loaded relationships
        List<AutoRedistributeSetting> settings = redistributeSettingRepo.findByEnabledTrueWithCompany();
        
        // Track current settings to identify canceled ones
        Set<Long> activeSettingIds = new HashSet<>();
        
        for (AutoRedistributeSetting setting : settings) {
            Long settingId = setting.getId();
            activeSettingIds.add(settingId);
            
            // Check if setting has been updated since last check
            LocalDateTime lastModified = redistributeLastUpdated.getOrDefault(settingId, LocalDateTime.MIN);
            LocalDateTime lastCheckTime = setting.getLastCheckTime();
            
            // Get frequency - use setting value or default
            int frequencySeconds = setting.getFrequencySeconds() != null ? 
                    setting.getFrequencySeconds() : (int)(defaultRedistributeDelay / 1000);
                    
            boolean needsReschedule = false;
            
            // If job doesn't exist or settings changed, schedule it
            if (!redistributeJobs.containsKey(settingId)) {
                needsReschedule = true;
            } else if (lastCheckTime != null && lastCheckTime.isAfter(lastModified)) {
                // Settings have been modified, reschedule
                needsReschedule = true;
            }
            
            if (needsReschedule) {
                // Cancel existing job if any
                cancelRedistributeJob(settingId);
                
                // Schedule new job with the setting's frequency
                ScheduledFuture<?> future = taskScheduler.schedule(
                    () -> redistributeJob.processRedistributeAsync(settingId),
                    new PeriodicTrigger(frequencySeconds, TimeUnit.SECONDS)
                );
                
                redistributeJobs.put(settingId, future);
                redistributeLastUpdated.put(settingId, LocalDateTime.now());
                
                log.info("Scheduled redistribute job for company ID {} with frequency {} seconds", 
                        setting.getCompany().getId(), frequencySeconds);
            }
        }
        
        // Cancel jobs that are no longer active
        for (Long id : new HashSet<>(redistributeJobs.keySet())) {
            if (!activeSettingIds.contains(id)) {
                cancelRedistributeJob(id);
            }
        }
    }
    
    @Transactional(readOnly = true)
    public void refreshAutoOrderJobs() {
        // Fetch all settings with eagerly loaded relationships
        List<AutoOrderSetting> settings = orderSettingRepo.findAllWithLocationAndCompany();
        
        // We'll only process enabled settings
        settings = settings.stream()
            .filter(AutoOrderSetting::isEnabled)
            .toList();
        
        // Track current settings to identify canceled ones
        Set<Long> activeSettingIds = new HashSet<>();
        
        for (AutoOrderSetting setting : settings) {
            Long settingId = setting.getId();
            activeSettingIds.add(settingId);
            
            // Check if setting has been updated since last check
            LocalDateTime lastModified = autoOrderLastUpdated.getOrDefault(settingId, LocalDateTime.MIN);
            LocalDateTime lastCheckTime = setting.getLastCheckTime();
            
            // Get frequency - use setting value or default
            int frequencySeconds = setting.getFrequencySeconds() != null ? 
                    setting.getFrequencySeconds() : (int)(defaultAutoOrderDelay / 1000);
                    
            boolean needsReschedule = false;
            
            // If job doesn't exist or settings changed, schedule it
            if (!autoOrderJobs.containsKey(settingId)) {
                needsReschedule = true;
            } else if (lastCheckTime != null && lastCheckTime.isAfter(lastModified)) {
                // Settings have been modified, reschedule
                needsReschedule = true;
            }
            
            if (needsReschedule) {
                // Cancel existing job if any
                cancelAutoOrderJob(settingId);
                
                // Schedule new job with the setting's frequency
                ScheduledFuture<?> future = taskScheduler.schedule(
                    () -> autoOrderService.processAutoOrderAsync(settingId),
                    new PeriodicTrigger(frequencySeconds, TimeUnit.SECONDS)
                );
                
                autoOrderJobs.put(settingId, future);
                autoOrderLastUpdated.put(settingId, LocalDateTime.now());
                
                if (setting.getLocation() != null && setting.getLocation().getCompany() != null) {
                    log.info("Scheduled auto-order job for location ID {} (company ID {}) with frequency {} seconds", 
                            setting.getLocation().getId(), 
                            setting.getLocation().getCompany().getId(), 
                            frequencySeconds);
                }
            }
        }
        
        // Cancel jobs that are no longer active
        for (Long id : new HashSet<>(autoOrderJobs.keySet())) {
            if (!activeSettingIds.contains(id)) {
                cancelAutoOrderJob(id);
            }
        }
    }
    
    private void cancelRedistributeJob(Long settingId) {
        ScheduledFuture<?> job = redistributeJobs.remove(settingId);
        if (job != null) {
            job.cancel(false);
            log.info("Canceled redistribute job for setting ID {}", settingId);
        }
    }
    
    private void cancelAutoOrderJob(Long settingId) {
        ScheduledFuture<?> job = autoOrderJobs.remove(settingId);
        if (job != null) {
            job.cancel(false);
            log.info("Canceled auto-order job for setting ID {}", settingId);
        }
    }
    
    // For manually triggering a refresh after settings change
    public void refreshAfterSettingsChange() {
        refreshAllJobs();
    }
}