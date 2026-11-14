package com.kakastudio.sms.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Decides whether the demo data is loaded. The schema lives in {@code db/migration}; the demo
 * accounts live in {@code db/demo} and are loaded unless {@code APP_DEMO_DATA=false}.
 */
@Configuration
public class DemoDataConfig {

  private static final String SCHEMA = "classpath:db/migration";
  private static final String DEMO = "classpath:db/demo";

  @Bean
  public FlywayConfigurationCustomizer demoDataLocations(
      @Value("${app.demo-data:true}") boolean demoData) {
    return configuration ->
        configuration
            .locations(locationsFor(demoData))
            // Switching the demo data off later must not stop the application from starting.
            .ignoreMigrationPatterns("*:future", "repeatable:missing");
  }

  public static String[] locationsFor(boolean demoData) {
    return demoData ? new String[] {SCHEMA, DEMO} : new String[] {SCHEMA};
  }
}
