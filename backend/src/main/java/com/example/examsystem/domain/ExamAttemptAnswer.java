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

/** Answer submitted for a question in an attempt. */
@Entity
@Table(name = "exam_attempt_answers")
public class ExamAttemptAnswer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "attempt_id", nullable = false)
  private ExamAttempt attempt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "paper_question_id", nullable = false)
  private ExamPaperQuestion paperQuestion;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String userAnswer;

  @Column(nullable = false)
  private boolean correct;

  public Long getId() {
    return id;
  }

  public ExamAttempt getAttempt() {
    return attempt;
  }

  public void setAttempt(ExamAttempt attempt) {
    this.attempt = attempt;
  }

  public ExamPaperQuestion getPaperQuestion() {
    return paperQuestion;
  }

  public void setPaperQuestion(ExamPaperQuestion paperQuestion) {
    this.paperQuestion = paperQuestion;
  }

  public String getUserAnswer() {
    return userAnswer;
  }

  public void setUserAnswer(String userAnswer) {
    this.userAnswer = userAnswer;
  }

  public boolean isCorrect() {
    return correct;
  }

  public void setCorrect(boolean correct) {
    this.correct = correct;
  }
}
