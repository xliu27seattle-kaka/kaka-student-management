package com.kakastudio.sms.common.error;

import com.kakastudio.sms.security.SessionRevokedException;
import com.kakastudio.sms.user.EmailTakenException;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Turns every failure raised while handling a request into an {@link ApiError}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** Name PostgreSQL gives the unique constraint on users.email. */
  private static final String USERS_EMAIL_CONSTRAINT = "users_email_key";

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiError> handleApiException(ApiException ex) {
    return respond(ex.status(), ex.code(), ex.getMessage(), List.of());
  }

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

  /** A path or query value of the wrong type, for example a status that does not exist. */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return respond(
        HttpStatus.BAD_REQUEST,
        ErrorCode.INVALID_PARAMETER,
        "The value of '" + ex.getName() + "' is not valid",
        List.of());
  }

  /**
   * The database refused a write that the service's own checks let through, which happens when two
   * requests race (two registrations with one email). The constraint is the last line of defence;
   * the answer is a conflict, never a crash, and never database details.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
    String cause = String.valueOf(NestedExceptionUtils.getMostSpecificCause(ex).getMessage());
    log.warn("Data integrity violation: {}", cause);
    if (cause.contains(USERS_EMAIL_CONSTRAINT)) {
      return respond(
          HttpStatus.CONFLICT, ErrorCode.EMAIL_TAKEN, EmailTakenException.MESSAGE, List.of());
    }
    return respond(
        HttpStatus.CONFLICT,
        ErrorCode.DATA_CONFLICT,
        "The request conflicts with existing data",
        List.of());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
    return respond(
        HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, "You are not allowed to do this", List.of());
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
    if (ex instanceof SessionRevokedException || ex.getCause() instanceof SessionRevokedException) {
      return respond(
          HttpStatus.UNAUTHORIZED,
          ErrorCode.SESSION_REVOKED,
          "This account is no longer active",
          List.of());
    }
    return respond(
        HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED, "Sign in to continue", List.of());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleOther(Exception ex) {
    if (ex instanceof ErrorResponse errorResponse) {
      // Spring's own request errors (unknown path, wrong method, unsupported media type ...).
      String detail = errorResponse.getBody().getDetail();
      return respond(
          errorResponse.getStatusCode(),
          ErrorCode.REQUEST_FAILED,
          detail != null ? detail : "The request could not be processed",
          List.of());
    }
    // Never leak internals: the details go to the log, the caller gets a fixed message.
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
