package com.kakastudio.sms.common.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Background work: the email sender threads and the scheduled outbox sweep. */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

  /** A small, bounded pool: emails are few, and a full queue must not eat memory. */
  @Bean
  public Executor emailExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(200);
    executor.setThreadNamePrefix("email-");
    executor.initialize();
    return executor;
  }
}
