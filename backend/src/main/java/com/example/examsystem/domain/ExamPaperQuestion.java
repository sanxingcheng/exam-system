package com.example.examsystem.domain;

import com.example.examsystem.domain.Enums.QuestionType;
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

/** Immutable question snapshot used by historical exam papers. */
@Entity
@Table(name = "exam_paper_questions")
public class ExamPaperQuestion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "paper_id", nullable = false)
  private ExamPaper paper;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "question_id", nullable = false)
  private Question question;

  @Column(nullable = false)
  private int questionOrder;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private QuestionType type;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String contentSnapshot;

  @Column(columnDefinition = "TEXT")
  private String optionsSnapshot;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String answerSnapshot;

  public Long getId() {
    return id;
  }

  public ExamPaper getPaper() {
    return paper;
  }

  public void setPaper(ExamPaper paper) {
    this.paper = paper;
  }

  public Question getQuestion() {
    return question;
  }

  public void setQuestion(Question question) {
    this.question = question;
  }

  public int getQuestionOrder() {
    return questionOrder;
  }

  public void setQuestionOrder(int questionOrder) {
    this.questionOrder = questionOrder;
  }

  public QuestionType getType() {
    return type;
  }

  public void setType(QuestionType type) {
    this.type = type;
  }

  public String getContentSnapshot() {
    return contentSnapshot;
  }

  public void setContentSnapshot(String contentSnapshot) {
    this.contentSnapshot = contentSnapshot;
  }

  public String getOptionsSnapshot() {
    return optionsSnapshot;
  }

  public void setOptionsSnapshot(String optionsSnapshot) {
    this.optionsSnapshot = optionsSnapshot;
  }

  public String getAnswerSnapshot() {
    return answerSnapshot;
  }

  public void setAnswerSnapshot(String answerSnapshot) {
    this.answerSnapshot = answerSnapshot;
  }
}
