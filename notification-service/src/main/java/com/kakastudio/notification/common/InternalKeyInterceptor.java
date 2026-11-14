package com.kakastudio.notification.common;

import com.kakastudio.notification.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Lets a request through only when it carries the shared key in {@code X-Internal-Key}. */
@Component
public class InternalKeyInterceptor implements HandlerInterceptor {

  public static final String HEADER = "X-Internal-Key";

  private final byte[] expectedKey;

  public InternalKeyInterceptor(AppProperties properties) {
    this.expectedKey = properties.internalApiKey().getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    String presented = request.getHeader(HEADER);
    if (presented == null
        || !MessageDigest.isEqual(presented.getBytes(StandardCharsets.UTF_8), expectedKey)) {
      throw new InvalidInternalKeyException();
    }
    return true;
  }
}
