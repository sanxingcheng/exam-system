package com.example.examsystem.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Converts exceptions to consistent JSON and keeps logs structured. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusiness(
      BusinessException exception, HttpServletRequest request) {
    log.warn("business_error path={} message={}", request.getRequestURI(), exception.getMessage());
    return ResponseEntity.badRequest().body(ApiResponse.error(exception.getMessage()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleDenied(
      AccessDeniedException exception, HttpServletRequest request) {
    log.warn("access_denied path={} message={}", request.getRequestURI(), exception.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("没有访问权限"));
  }

  @ExceptionHandler(AsyncRequestNotUsableException.class)
  public void handleClientAbort(AsyncRequestNotUsableException exception, HttpServletRequest request) {
    log.debug("client_aborted_request path={} message={}", request.getRequestURI(), exception.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(
      Exception exception, HttpServletRequest request) {
    log.error("unexpected_error path={}", request.getRequestURI(), exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("系统异常，请稍后重试"));
  }
}
