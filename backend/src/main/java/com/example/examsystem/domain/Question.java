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
import java.time.Instant;

/** Normalized question record independent of the import source format. */
@Entity
@Table(name = "questions")
public class Question {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "question_bank_id", nullable = false)
  private QuestionBank questionBank;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private QuestionType type;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(length = 128)
  private String sourceSection;

  @Column(length = 128)
  private String knowledgeArea;

  @Column(nullable = false)
  private boolean hasAnswer;

  @Column(nullable = false)
  private boolean answerLocked;

  @Column(length = 128)
  private String externalCode;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() {
    return id;
  }

  public QuestionBank getQuestionBank() {
    return questionBank;
  }

  public void setQuestionBank(QuestionBank questionBank) {
    this.questionBank = questionBank;
  }

  public QuestionType getType() {
    return type;
  }

  public void setType(QuestionType type) {
    this.type = type;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getSourceSection() {
    return sourceSection;
  }

  public void setSourceSection(String sourceSection) {
    this.sourceSection = sourceSection;
  }

  public String getKnowledgeArea() {
    return knowledgeArea;
  }

  public void setKnowledgeArea(String knowledgeArea) {
    this.knowledgeArea = knowledgeArea;
  }

  public boolean isHasAnswer() {
    return hasAnswer;
  }

  public void setHasAnswer(boolean hasAnswer) {
    this.hasAnswer = hasAnswer;
  }

  public boolean isAnswerLocked() {
    return answerLocked;
  }

  public void setAnswerLocked(boolean answerLocked) {
    this.answerLocked = answerLocked;
  }

  public String getExternalCode() {
    return externalCode;
  }

  public void setExternalCode(String externalCode) {
    this.externalCode = externalCode;
  }
}
