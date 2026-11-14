package com.kakastudio.notification.common;

import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns every failure into an {@link ApiError}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    List<ApiError.FieldError> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new ApiError.FieldError(error.getField(), error.getDefaultMessage()))
            .sorted(Comparator.comparing(ApiError.FieldError::field))
            .toList();
    return respond(
        HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Request is not valid", fieldErrors);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
    return respond(
        HttpStatus.BAD_REQUEST,
        ErrorCode.MALFORMED_REQUEST,
        "Request body is missing or is not valid JSON",
        List.of());
  }

  @ExceptionHandler(InvalidInternalKeyException.class)
  public ResponseEntity<ApiError> handleInvalidKey(InvalidInternalKeyException ex) {
    return respond(
        HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_INTERNAL_KEY, ex.getMessage(), List.of());
  }

  @ExceptionHandler(MailDeliveryException.class)
  public ResponseEntity<ApiError> handleMailDelivery(MailDeliveryException ex) {
    log.warn("Mail delivery failed (permanent={}): {}", ex.isPermanent(), ex.getMessage());
    return ex.isPermanent()
        ? respond(
            HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.MAIL_REJECTED, ex.getMessage(), List.of())
        : respond(
            HttpStatus.BAD_GATEWAY, ErrorCode.MAIL_DELIVERY_FAILED, ex.getMessage(), List.of());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleOther(Exception ex) {
    if (ex instanceof ErrorResponse errorResponse) {
      // Spring's own request errors (unknown path, wrong method, unsupported media type ...).
      return respond(
          errorResponse.getStatusCode(),
          ErrorCode.REQUEST_FAILED,
          errorResponse.getBody().getDetail(),
          List.of());
    }
    log.error("Unexpected error", ex);
    return respond(
        HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "Unexpected error", List.of());
  }

  private static ResponseEntity<ApiError> respond(
      HttpStatusCode status,
      ErrorCode code,
      String message,
      List<ApiError.FieldError> fieldErrors) {
    return ResponseEntity.status(status).body(ApiError.of(status, code, message, fieldErrors));
  }
}
