package com.example.examsystem.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Per-user wrong question record scoped by question bank. */
@Entity
@Table(name = "wrong_questions")
public class WrongQuestion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "question_bank_id", nullable = false)
  private QuestionBank questionBank;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "question_id", nullable = false)
  private Question question;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(nullable = false)
  private int wrongCount;

  @Column(nullable = false)
  private boolean mastered;

  @Column(nullable = false)
  private Instant lastWrongAt = Instant.now();

  public Long getId() {
    return id;
  }

  public QuestionBank getQuestionBank() {
    return questionBank;
  }

  public void setQuestionBank(QuestionBank questionBank) {
    this.questionBank = questionBank;
  }

  public Question getQuestion() {
    return question;
  }

  public void setQuestion(Question question) {
    this.question = question;
  }

  public AppUser getUser() {
    return user;
  }

  public void setUser(AppUser user) {
    this.user = user;
  }

  public int getWrongCount() {
    return wrongCount;
  }

  public void setWrongCount(int wrongCount) {
    this.wrongCount = wrongCount;
  }

  public boolean isMastered() {
    return mastered;
  }

  public void setMastered(boolean mastered) {
    this.mastered = mastered;
  }

  public void markWrong() {
    wrongCount++;
    mastered = false;
    lastWrongAt = Instant.now();
  }
}
