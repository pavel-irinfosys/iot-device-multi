package com.irinfosys.websocket.multi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class DeviceUploadThreadPoolConfig {

  @Bean("deviceUploadExecutor")
  public TaskExecutor deviceUploadExecutor() {
    final ThreadPoolTaskExecutor executor =
        new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(20);
    executor.setMaxPoolSize(40);
    executor.setQueueCapacity(1000);
    executor.setThreadNamePrefix("device-upload-");

    // When the queue is full, the scheduler thread executes the task.
    executor.setRejectedExecutionHandler(
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    executor.initialize();
    return executor;
  }
}
