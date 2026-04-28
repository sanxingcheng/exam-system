package com.example.examsystem.domain;

import com.example.examsystem.domain.Enums.AnswerSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Standard answer and explanation for a question. */
@Entity
@Table(name = "question_answers")
public class QuestionAnswer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "question_id", nullable = false, unique = true)
  private Question question;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String answerText;

  @Column(columnDefinition = "TEXT")
  private String explanation;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private AnswerSource source;

  @Column(nullable = false)
  private boolean locked;

  public Long getId() {
    return id;
  }

  public Question getQuestion() {
    return question;
  }

  public void setQuestion(Question question) {
    this.question = question;
  }

  public String getAnswerText() {
    return answerText;
  }

  public void setAnswerText(String answerText) {
    this.answerText = answerText;
  }

  public String getExplanation() {
    return explanation;
  }

  public void setExplanation(String explanation) {
    this.explanation = explanation;
  }

  public AnswerSource getSource() {
    return source;
  }

  public void setSource(AnswerSource source) {
    this.source = source;
  }

  public boolean isLocked() {
    return locked;
  }

  public void setLocked(boolean locked) {
    this.locked = locked;
  }
}
