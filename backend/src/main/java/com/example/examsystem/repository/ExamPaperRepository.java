package com.example.examsystem.repository;

import com.example.examsystem.domain.ExamPaper;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamPaperRepository extends JpaRepository<ExamPaper, Long> {}
