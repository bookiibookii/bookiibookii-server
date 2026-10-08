package com.example.bookiibookii.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    @Test
    void notificationExecutorUsesCallerRunsPolicy() {
        Executor executor = new AsyncConfig().notiExecutor();
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;

        assertThat(taskExecutor.getThreadPoolExecutor().getRejectedExecutionHandler())
                .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);

        taskExecutor.shutdown();
    }
}
