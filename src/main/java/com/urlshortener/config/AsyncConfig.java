package com.urlshortener.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    public static final String CLICK_EXECUTOR = "clickRecordingExecutor";

    @Bean(name = CLICK_EXECUTOR)
    public Executor clickRecordingExecutor(AppProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getAsync().getClickCorePoolSize());
        executor.setMaxPoolSize(properties.getAsync().getClickMaxPoolSize());
        executor.setQueueCapacity(properties.getAsync().getClickQueueCapacity());
        executor.setThreadNamePrefix("click-record-");
        executor.setRejectedExecutionHandler((runnable, pool) ->
                org.slf4j.LoggerFactory.getLogger(AsyncConfig.class)
                        .warn("Click recording queue full; dropping click to protect redirect latency"));
        executor.initialize();
        return executor;
    }
}
