package com.rayvision.inventory_management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="location_integration_setting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationIntegrationSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One-to-One or Many-to-One, depends on your design
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", unique = true, nullable = false)
    private Location location;

    private String posApiUrl;

    /**
     * The frequent sync is an approach that runs every X seconds
     * (user specified). We store that here.
     */
    private Integer frequentSyncSeconds; // e.g. 300 (5 minutes)

    /**
     * Whether frequent sync is enabled or not
     */
    private boolean frequentSyncEnabled;

    /**
     * Whether the daily midnight sync is enabled
     */
    private boolean dailySyncEnabled;

    /**
     * We store the last time we fully synced for the frequent job
     */
    private LocalDateTime lastFrequentSyncTime;

    /**
     * The last page we reached in frequent sync
     * (if you want to handle multi-page)
     */
    private Integer lastFrequentPageSynced;

    /**
     * For the daily job
     */
    private LocalDateTime lastDailySyncTime;
    private Integer lastDailyPageSynced;

    // Possibly store “apiKey” or “username/password”
    private String apiKey;



}
