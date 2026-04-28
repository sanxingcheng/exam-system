package com.example.examsystem.repository;

import com.example.examsystem.domain.QuestionOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
  List<QuestionOption> findByQuestionIdOrderByOptionKeyAsc(Long questionId);
}
