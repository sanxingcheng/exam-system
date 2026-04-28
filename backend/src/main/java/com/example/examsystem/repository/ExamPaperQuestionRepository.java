package com.example.examsystem.repository;

import com.example.examsystem.domain.ExamPaperQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamPaperQuestionRepository extends JpaRepository<ExamPaperQuestion, Long> {
  List<ExamPaperQuestion> findByPaperIdOrderByQuestionOrderAsc(Long paperId);
}
