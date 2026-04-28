package com.example.examsystem.repository;

import com.example.examsystem.domain.ExamAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamAttemptAnswerRepository extends JpaRepository<ExamAttemptAnswer, Long> {}
