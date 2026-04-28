package com.example.examsystem.domain;

/** Shared domain enums. */
public final class Enums {

  private Enums() {}

  public enum UserRole {
    ADMIN,
    USER
  }

  public enum QuestionType {
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    SHORT_ANSWER,
    PRACTICAL
  }

  public enum BankStatus {
    ACTIVE,
    DISABLED
  }

  public enum AnswerSource {
    IMPORTED_LOCKED,
    ADMIN_FILLED
  }

  public enum AttemptStatus {
    IN_PROGRESS,
    SUBMITTED,
    EXPIRED
  }
}
