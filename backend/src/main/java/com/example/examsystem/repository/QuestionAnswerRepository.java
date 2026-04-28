package com.example.examsystem.repository;

import com.example.examsystem.domain.QuestionAnswer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {
  Optional<QuestionAnswer> findByQuestionId(Long questionId);
}
