package com.example.examsystem.domain;

import com.example.examsystem.domain.Enums.AttemptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** User exam attempt and score record. */
@Entity
@Table(name = "exam_attempts")
public class ExamAttempt {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "question_bank_id", nullable = false)
  private QuestionBank questionBank;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "paper_id", nullable = false)
  private ExamPaper paper;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private AttemptStatus status = AttemptStatus.IN_PROGRESS;

  @Column(nullable = false)
  private Instant startedAt = Instant.now();

  private Instant submittedAt;

  @Column(nullable = false)
  private int score;

  @Column(nullable = false)
  private int totalQuestions;

  @Column(nullable = false)
  private int correctQuestions;

  public Long getId() {
    return id;
  }

  public QuestionBank getQuestionBank() {
    return questionBank;
  }

  public void setQuestionBank(QuestionBank questionBank) {
    this.questionBank = questionBank;
  }

  public ExamPaper getPaper() {
    return paper;
  }

  public void setPaper(ExamPaper paper) {
    this.paper = paper;
  }

  public AppUser getUser() {
    return user;
  }

  public void setUser(AppUser user) {
    this.user = user;
  }

  public AttemptStatus getStatus() {
    return status;
  }

  public void setStatus(AttemptStatus status) {
    this.status = status;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(Instant submittedAt) {
    this.submittedAt = submittedAt;
  }

  public int getScore() {
    return score;
  }

  public void setScore(int score) {
    this.score = score;
  }

  public int getTotalQuestions() {
    return totalQuestions;
  }

  public void setTotalQuestions(int totalQuestions) {
    this.totalQuestions = totalQuestions;
  }

  public int getCorrectQuestions() {
    return correctQuestions;
  }

  public void setCorrectQuestions(int correctQuestions) {
    this.correctQuestions = correctQuestions;
  }
}
