package com.rayvision.inventory_management.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Value("${inventory.scheduler.auto-order.pool-size:5}")
    private int autoOrderPoolSize;
    
    @Value("${inventory.scheduler.redistribute.pool-size:5}")
    private int redistributePoolSize;
    
    @Value("${inventory.scheduler.queue-capacity:25}")
    private int queueCapacity;

    @Bean(name = "autoOrderExecutor")
    public Executor autoOrderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(autoOrderPoolSize);
        executor.setMaxPoolSize(autoOrderPoolSize * 2);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("auto-order-");
        executor.initialize();
        return executor;
    }
    
    @Bean(name = "redistributeExecutor")
    public Executor redistributeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(redistributePoolSize);
        executor.setMaxPoolSize(redistributePoolSize * 2);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("redistribute-");
        executor.initialize();
        return executor;
    }
}