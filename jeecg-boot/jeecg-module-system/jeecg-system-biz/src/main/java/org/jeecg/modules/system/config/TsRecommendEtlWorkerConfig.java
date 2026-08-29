package org.jeecg.modules.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/** 推荐 ETL 本地执行线程池配置。 */
@Configuration
public class TsRecommendEtlWorkerConfig {

    /** 创建有界执行线程池，避免任务洪峰耗尽应用线程。 */
    @Bean("tsRecommendEtlExecutor")
    public Executor tsRecommendEtlExecutor(TsRecommendEtlConfig config) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getWorkerCoreSize());
        executor.setMaxPoolSize(config.getWorkerMaxSize());
        executor.setQueueCapacity(config.getWorkerQueueCapacity());
        executor.setThreadNamePrefix("recommend-etl-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
