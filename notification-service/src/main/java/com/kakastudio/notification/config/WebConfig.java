package com.kakastudio.notification.config;

import com.kakastudio.notification.common.InternalKeyInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final InternalKeyInterceptor internalKeyInterceptor;

  public WebConfig(InternalKeyInterceptor internalKeyInterceptor) {
    this.internalKeyInterceptor = internalKeyInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(internalKeyInterceptor).addPathPatterns("/api/**");
  }
}
