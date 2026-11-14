package com.kakastudio.sms.security;

import com.kakastudio.sms.common.config.ClockConfig;
import com.kakastudio.sms.security.service.impl.JwtTokenService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** The real security setup, for controller slice tests that need authentication to work. */
@Configuration
@Import({
  SecurityConfig.class,
  JwtConfig.class,
  JwtTokenService.class,
  ApiErrorSecurityHandler.class,
  ClockConfig.class
})
public class SecurityTestConfig {}
