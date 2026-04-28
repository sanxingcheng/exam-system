package com.example.examsystem.common;

/** Exception for expected business rule violations. */
public class BusinessException extends RuntimeException {

  public BusinessException(String message) {
    super(message);
  }
}
