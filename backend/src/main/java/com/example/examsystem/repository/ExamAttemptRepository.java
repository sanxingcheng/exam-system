package com.example.examsystem.repository;

import com.example.examsystem.domain.ExamAttempt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
  List<ExamAttempt> findByUserIdAndQuestionBankIdOrderByStartedAtDesc(Long userId, Long questionBankId);
}
