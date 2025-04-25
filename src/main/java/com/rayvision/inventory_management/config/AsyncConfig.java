package com.rayvision.inventory_management.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${inventory.scheduler.auto-order.pool-size:3}")
    private int autoOrderPoolSize;
    
    @Value("${inventory.scheduler.redistribute.pool-size:3}")
    private int redistributePoolSize;
    
    @Value("${inventory.scheduler.queue-capacity:25}")
    private int queueCapacity;

    @Bean(name = "autoOrderExecutor")
    public Executor autoOrderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(autoOrderPoolSize);
        executor.setMaxPoolSize(autoOrderPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("AutoOrder-");
        executor.initialize();
        return executor;
    }
    
    @Bean(name = "redistributeExecutor")
    public Executor redistributeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(redistributePoolSize);
        executor.setMaxPoolSize(redistributePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("Redistribute-");
        executor.initialize();
        return executor;
    }
}