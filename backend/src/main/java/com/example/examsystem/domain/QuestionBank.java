package com.example.examsystem.domain;

import com.example.examsystem.domain.Enums.BankStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Top-level isolation boundary for questions, exams and user records. */
@Entity
@Table(name = "question_banks")
public class QuestionBank {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 128)
  private String name;

  @Column(nullable = false, length = 64)
  private String bankType;

  @Column(length = 512)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private BankStatus status = BankStatus.ACTIVE;

  @Column(nullable = false)
  private int defaultDurationMinutes = 90;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBankType() {
    return bankType;
  }

  public void setBankType(String bankType) {
    this.bankType = bankType;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BankStatus getStatus() {
    return status;
  }

  public int getDefaultDurationMinutes() {
    return defaultDurationMinutes;
  }

  public void setDefaultDurationMinutes(int defaultDurationMinutes) {
    this.defaultDurationMinutes = defaultDurationMinutes;
  }
}
