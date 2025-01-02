package me.amlu.shop.amlume_shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ConcurrencyConfig {
    
    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }
}