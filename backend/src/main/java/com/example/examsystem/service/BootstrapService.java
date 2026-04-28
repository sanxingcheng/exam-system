package com.example.examsystem.service;

import com.example.examsystem.domain.QuestionBank;
import com.example.examsystem.repository.QuestionBankRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Initializes mandatory built-in data without importing PDF repeatedly. */
@Component
public class BootstrapService implements ApplicationRunner {

  public static final String BLOCKCHAIN_BANK_NAME = "区块链应用操作员三级";

  private final AuthService authService;
  private final QuestionBankRepository bankRepository;

  public BootstrapService(AuthService authService, QuestionBankRepository bankRepository) {
    this.authService = authService;
    this.bankRepository = bankRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    authService.ensureAdminExists();
    bankRepository
        .findByName(BLOCKCHAIN_BANK_NAME)
        .orElseGet(
            () -> {
              QuestionBank bank = new QuestionBank();
              bank.setName(BLOCKCHAIN_BANK_NAME);
              bank.setBankType("BLOCKCHAIN_OPERATOR_LEVEL3_PDF");
              bank.setDescription("区块链应用操作员三级指导手册题库");
              bank.setDefaultDurationMinutes(90);
              return bankRepository.save(bank);
            });
  }
}
