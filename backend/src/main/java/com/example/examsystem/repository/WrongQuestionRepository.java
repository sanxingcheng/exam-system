package com.example.examsystem.repository;

import com.example.examsystem.domain.WrongQuestion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WrongQuestionRepository extends JpaRepository<WrongQuestion, Long> {
  Optional<WrongQuestion> findByQuestionBankIdAndQuestionIdAndUserId(
      Long questionBankId, Long questionId, Long userId);

  List<WrongQuestion> findByQuestionBankIdAndUserIdAndMasteredFalseOrderByWrongCountDesc(
      Long questionBankId, Long userId);
}
