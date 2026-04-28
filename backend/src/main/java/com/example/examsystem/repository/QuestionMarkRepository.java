package com.example.examsystem.repository;

import com.example.examsystem.domain.QuestionMark;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionMarkRepository extends JpaRepository<QuestionMark, Long> {
  Optional<QuestionMark> findByQuestionBankIdAndQuestionIdAndUserId(
      Long questionBankId, Long questionId, Long userId);

  List<QuestionMark> findByQuestionBankIdAndUserIdAndHardTrue(Long questionBankId, Long userId);

  List<QuestionMark> findByQuestionBankIdAndQuestionIdInAndUserId(
      Long questionBankId, Collection<Long> questionIds, Long userId);
}
