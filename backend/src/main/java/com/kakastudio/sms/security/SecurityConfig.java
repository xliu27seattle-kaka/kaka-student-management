package com.kakastudio.sms.security;

import com.kakastudio.sms.user.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless API security: every request carries a bearer token. The first path segment after {@code
 * /api/v1} decides which role may enter.
 */
@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain apiSecurity(
      HttpSecurity http,
      ActiveUserJwtAuthenticationConverter authenticationConverter,
      ApiErrorSecurityHandler errorHandler)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable) // no cookies, so nothing for CSRF to ride on
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth ->
            auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/register")
                .permitAll()
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**")
                .permitAll()
                .requestMatchers("/api/v1/admin/**")
                .hasRole("ADMIN")
                .requestMatchers("/api/v1/professor/**")
                .hasRole("PROFESSOR")
                .requestMatchers("/api/v1/student/**")
                .hasRole("STUDENT")
                .anyRequest()
                .authenticated())
        .oauth2ResourceServer(oauth ->
            oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter))
                .authenticationEntryPoint(errorHandler)
                .accessDeniedHandler(errorHandler))
        .exceptionHandling(errors ->
            errors.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler));
    return http.build();
  }

  @Bean
  public ActiveUserJwtAuthenticationConverter activeUserJwtAuthenticationConverter(
      UserRepository users) {
    return new ActiveUserJwtAuthenticationConverter(users);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
