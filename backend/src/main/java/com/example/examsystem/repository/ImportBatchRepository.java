package com.example.examsystem.repository;

import com.example.examsystem.domain.ImportBatch;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {
  boolean existsByQuestionBankIdAndImporterTypeAndFileSha256(
      Long questionBankId, String importerType, String fileSha256);

  Optional<ImportBatch> findByQuestionBankIdAndImporterTypeAndFileSha256(
      Long questionBankId, String importerType, String fileSha256);
}
