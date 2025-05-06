/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Virtual Thread Pool Configuration
 * <p>
 * This class configures the thread pool for the application.
 * It uses the ThreadPoolTaskExecutor to create and manage threads for various tasks.
 * </p>
 * <p>
 * The thread pool is configured with the following settings:
 * <ul>
 *     <li>Core pool size: The number of threads to keep in the pool, even if they are idle.</li>
 *     <li>Max pool size: The maximum number of threads to allow in the pool.</li>
 *     <li>Queue capacity: The maximum number of tasks that can be queued for execution.</li>
 *     <li>Thread name prefix: A prefix for the names of the threads in the pool.</li>
 * </ul>
 * </p>
 * <p>
 * The thread pool is configured for I/O-bound tasks, such as database operations, file I/O, and network I/O.
 * </p>
 * <p>
 * The thread pool is configured for CPU-bound tasks, such as image processing, data analysis, and mathematical computations.
 * </p>
 * <p>
 * The thread pool is configured for mixed tasks, such as web requests, message processing, and event handling.
 * </p>
 * <p>
 * The thread pool is configured for background tasks, such as scheduled tasks, batch processing, and data synchronization.
 * </p>
 * <p>
 * The thread pool is configured for foreground tasks, such as user requests, real-time processing, and interactive applications.
 * </p>
 * <p>
 * The thread pool is configured for high-priority tasks, such as emergency alerts, critical notifications, and urgent messages.
 * </p>
 * <p>
 * The thread pool is configured for low-priority tasks, such as background maintenance, periodic updates, and non-urgent notifications.
 * </p>
 * <p>
 * The thread pool is configured for short-lived tasks, such as temporary jobs, one-time operations, and quick tasks.
 * </p>
 * <p>
 * The thread pool is configured for long-lived tasks, such as continuous operations, ongoing processes, and persistent tasks.
 * </p>
 * <p>
 * The thread pool is configured for scalable tasks, such as dynamic workloads, adaptive processing, and elastic tasks.
 * </p>
 * <p>
 * The thread pool is configured for fixed tasks, such as static workloads, predefined operations, and constant tasks.
 * </p>
 *
 * @author Daniel Itiro Tikamori
 * @version 1.0
 * @description ThreadPoolConfig
 * @since 2025-02-28
 */
@Configuration
public class ThreadPoolConfig {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolConfig.class);

    @Value("${threadpool.io.core-pool-size:10}")
    private int ioCorePoolSize;

    @Value("${threadpool.io.max-pool-size:100}")
    private int ioMaxPoolSize;

    @Value("${threadpool.io.queue-capacity:1000}")
    private int ioQueueCapacity;

    @Value("${threadpool.io.thread-name-prefix:io-task-}")
    private String ioThreadNamePrefix;

    @Value("${threadpool.token-background.core-pool-size:10}")
    private int tokenBackgroundCorePoolSize;

    @Value("${threadpool.token-background.max-pool-size:100}")
    private int tokenBackgroundMaxPoolSize;

    @Value("${threadpool.token-background.queue-capacity:1000}")
    private int tokenBackgroundQueueCapacity;

    @Value("${threadpool.token-background.thread-name-prefix:token-background-}")
    private String tokenBackgroundThreadNamePrefix;

    @Value("${threadpool.token-cpu.queue-capacity:100}")
    private int tokenCpuQueueCapacity;

    @Value("${threadpool.token-cpu.thread-name-prefix:token-cpu-}")
    private String tokenCpuThreadNamePrefix;

    @Value("${threadpool.scalable-token.keep-alive-seconds:60}")
    private int scalableTokenKeepAliveSeconds;

    @Value("${threadpool.scalable-token.thread-name-prefix:scalable-token-processor-}")
    private String scalableTokenThreadNamePrefix;

    @Bean("ioTaskExecutor")
    public ThreadPoolTaskExecutor ioTaskExecutor() {
        return createExecutor(ioCorePoolSize, ioMaxPoolSize, ioQueueCapacity, ioThreadNamePrefix);
    }

    @Bean("tokenBackgroundTasksExecutor")
    public ThreadPoolTaskExecutor tokenBackgroundTasksExecutor() {
        return createExecutor(tokenBackgroundCorePoolSize, tokenBackgroundMaxPoolSize, tokenBackgroundQueueCapacity, tokenBackgroundThreadNamePrefix);
    }

    @Bean("tokenCpuTasksExecutor")
    public ThreadPoolTaskExecutor tokenCpuTasksExecutor() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createExecutor(availableProcessors, availableProcessors, tokenCpuQueueCapacity, tokenCpuThreadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Bean("scalableTokenProcessingExecutor")
    public ThreadPoolTaskExecutor scalableTokenProcessingExecutor() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createExecutor(availableProcessors, availableProcessors, tokenCpuQueueCapacity, scalableTokenThreadNamePrefix);
        executor.setKeepAliveSeconds(scalableTokenKeepAliveSeconds);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    private ThreadPoolTaskExecutor createExecutor(int corePoolSize, int maxPoolSize, int queueCapacity, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // Rejection policy
        executor.setWaitForTasksToCompleteOnShutdown(true); // Wait for tasks to complete on shutdown
        executor.setAwaitTerminationSeconds(60); // Wait for up to 60 seconds
        executor.setThreadFactory(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setUncaughtExceptionHandler((t, e) -> log.error("Uncaught exception in thread: {}", t.getName(), e));
            return thread;
        });
        executor.initialize();
        return executor;
    }
}