package com.example.examsystem.repository;

import com.example.examsystem.domain.QuestionBank;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {
  Optional<QuestionBank> findByName(String name);
}
