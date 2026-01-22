package com.smim.backend.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 * AI 단어 추출 등의 장시간 작업을 비동기로 처리하기 위한 설정
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);  // 기본 스레드 수
        executor.setMaxPoolSize(5);   // 최대 스레드 수
        executor.setQueueCapacity(10); // 대기 큐 크기
        executor.setThreadNamePrefix("async-executor-");
        executor.initialize();
        log.info("Async Executor 초기화 완료");
        return executor;
    }
}
