package com.kakastudio.sms.common.error;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatusCode;

/** The one error body shape of the API. */
public record ApiError(
    int status, String code, String message, List<FieldError> fieldErrors, Instant timestamp) {

  public record FieldError(String field, String message) {}

  public static ApiError of(HttpStatusCode status, ErrorCode code, String message) {
    return of(status, code, message, List.of());
  }

  public static ApiError of(
      HttpStatusCode status, ErrorCode code, String message, List<FieldError> fieldErrors) {
    return new ApiError(status.value(), code.name(), message, fieldErrors, Instant.now());
  }
}
