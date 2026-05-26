package com.ds.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${app.email.pool.min-size:3}")
    private int minSize;

    @Value("${app.email.pool.max-size:10}")
    private int maxSize;

    @Value("${app.email.pool.queue-capacity:100}")
    private int queueCapacity;

    @Bean(name = "emailTaskExecutor")
    public ThreadPoolTaskExecutor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(minSize);    // start small, scaler grows it
        executor.setMaxPoolSize(maxSize);     // scaler can grow up to hardMaxSize
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("email-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}