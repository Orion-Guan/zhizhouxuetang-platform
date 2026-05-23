package com.tianji.promotion.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@Slf4j
public class PromotionConfig {

    /**
     * 创建用于生成兑换码的线程池执行器
     * 
     * 该线程池适用于兑换码生成等异步任务场景，采用适中的线程配置以平衡性能和资源消耗。
     * 
     * 配置参数：
     * - 核心线程数：2
     * - 最大线程数：5
     * - 队列容量：100
     * - 拒绝策略：CallerRunsPolicy（由调用线程执行被拒绝的任务，提供背压机制）
     * 
     * @return Executor 配置完成并初始化的线程池执行器实例
     */
    @Bean
    public Executor generateExchangeCodeExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("generateExchangeCodeExecutor-");
        executor.initialize();
        return executor;
    }
}
